package bisq.network.p2p.node.authorization.token.equi_hash;

import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.util.ByteArrayUtils;
import bisq.common.util.MathUtils;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenService;
import bisq.network.p2p.node.authorization.token.hash_cash.HashCashToken;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

/**
 * TODO: Implement once EquiHash is production ready
 */
@Slf4j
public class EquiHashTokenService extends AuthorizationTokenService<EquiHashToken> {

    public final static double MIN_MESSAGE_COST = 0.01;
    public final static double MIN_LOAD = 0.01;
    public final static int MIN_DIFFICULTY = 128;  // 2^7 = 128; 3 ms on old CPU, 1 ms on high-end CPU. Would result in an average time of 1-5 ms on high-end CPU
    public final static int TARGET_DIFFICULTY = 65536;  // 2^16 = 262144; 1000 ms on old CPU, 60-140 ms on high-end CPU. Would result in an average time of 100-150 ms on high-end CPU
    public final static int MAX_DIFFICULTY = 1048576;  // 2^20 = 1048576; Would result in an average time 0.5-2 sec on high-end CPU
    public final static int DIFFICULTY_TOLERANCE = 50_000;

    private final EquihashProofOfWorkService proofOfWorkService;

    public EquiHashTokenService(EquihashProofOfWorkService proofOfWorkService) {
        this.proofOfWorkService = proofOfWorkService;
    }

    @Override
    public EquiHashToken createToken(EnvelopePayloadMessage message,
                                     NetworkLoad networkLoad,
                                     String peerAddress,
                                     int messageCounter) {
        var proofOfWork = proofOfWorkService.mint(
                getPayload(message),
                getChallenge(peerAddress, messageCounter),
                calculateDifficulty(message, networkLoad));
        var token = new EquiHashToken(proofOfWork, messageCounter);
        return token;
    }

    @Override
    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                Optional<NetworkLoad> previousNetworkLoad,
                                String connectionId,
                                String myAddress) {
        HashCashToken hashCashToken = (HashCashToken) authorizationToken;
        ProofOfWork proofOfWork = hashCashToken.getProofOfWork();

        // Verify payload
        byte[] payload = getPayload(message);
        if (!Arrays.equals(payload, proofOfWork.getPayload())) {
            log.warn("Message payload not matching proof of work payload. " +
                            "getPayload(message)={}; proofOfWork.getPayload()={}; " +
                            "getPayload(message).length={}; proofOfWork.getPayload().length={}",
                    Hex.encode(payload), Hex.encode(proofOfWork.getPayload()),
                    payload.length, proofOfWork.getPayload().length);
            return false;
        }

        // Verify difficulty
        if (isDifficultyInvalid(message, proofOfWork.getDifficulty(), currentNetworkLoad, previousNetworkLoad)) {
            return false;
        }
        return proofOfWorkService.verify(proofOfWork);
    }

    // We check the difficulty used for the proof of work if it matches the current network load or if available the
    // previous network load. If the difference is inside a tolerance range we consider it still valid, but it should
    // be investigated why that happens, thus we log those cases.
    private boolean isDifficultyInvalid(EnvelopePayloadMessage message,
                                        double proofOfWorkDifficulty,
                                        NetworkLoad currentNetworkLoad,
                                        Optional<NetworkLoad> previousNetworkLoad) {
        log.debug("isDifficultyInvalid/currentNetworkLoad: message.getCostFactor()={}, networkLoad.getValue()={}",
                message.getCostFactor(), currentNetworkLoad.getLoad());
        double expectedDifficulty = calculateDifficulty(message, currentNetworkLoad);
        if (proofOfWorkDifficulty >= expectedDifficulty) {
            // We don't want to call calculateDifficulty with the previousNetworkLoad if we are not in dev mode.
            if (DevMode.isDevMode() && proofOfWorkDifficulty > expectedDifficulty && previousNetworkLoad.isPresent()) {
                // Might be that the difficulty was using the previous network load
                double expectedPreviousDifficulty = calculateDifficulty(message, previousNetworkLoad.get());
                if (proofOfWorkDifficulty != expectedPreviousDifficulty) {
                    log.warn("Unexpected high difficulty provided. This might be a bug (but valid as provided difficulty is larger as expected): " +
                                    "expectedDifficulty={}; expectedPreviousDifficulty={}; proofOfWorkDifficulty={}",
                            expectedDifficulty, expectedPreviousDifficulty, proofOfWorkDifficulty);
                }
            }
            return false;
        }

        double missing = expectedDifficulty - proofOfWorkDifficulty;
        double deviationToTolerance = MathUtils.roundDouble(missing / DIFFICULTY_TOLERANCE * 100, 2);
        double deviationToExpectedDifficulty = MathUtils.roundDouble(missing / expectedDifficulty * 100, 2);
        if (previousNetworkLoad.isEmpty()) {
            log.debug("No previous network load available");
            if (missing <= DIFFICULTY_TOLERANCE) {
                log.info("Difficulty of current network load deviates from the proofOfWork difficulty but is inside the tolerated range.\n" +
                                "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                        deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
                return false;
            }

            log.warn("Difficulty of current network load deviates from the proofOfWork difficulty and is outside the tolerated range.\n" +
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
            return true;
        }

        log.debug("isDifficultyInvalid/previousNetworkLoad: message.getCostFactor()={}, networkLoad.getValue()={}",
                message.getCostFactor(), previousNetworkLoad.get().getLoad());
        double expectedPreviousDifficulty = calculateDifficulty(message, previousNetworkLoad.get());
        if (proofOfWorkDifficulty >= expectedPreviousDifficulty) {
            log.debug("Difficulty of previous network load is correct");
            if (proofOfWorkDifficulty > expectedPreviousDifficulty) {
                log.warn("Unexpected high difficulty provided. This might be a bug (but valid as provided difficulty is larger as expected): " +
                                "expectedPreviousDifficulty={}; proofOfWorkDifficulty={}",
                        expectedPreviousDifficulty, proofOfWorkDifficulty);
            }
            return false;
        }

        if (missing <= DIFFICULTY_TOLERANCE) {
            log.info("Difficulty of current network load deviates from the proofOfWork difficulty but is inside the tolerated range.\n" +
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
            return false;
        }

        double missingUsingPrevious = expectedPreviousDifficulty - proofOfWorkDifficulty;
        if (missingUsingPrevious <= DIFFICULTY_TOLERANCE) {
            deviationToTolerance = MathUtils.roundDouble(missingUsingPrevious / DIFFICULTY_TOLERANCE * 100, 2);
            deviationToExpectedDifficulty = MathUtils.roundDouble(missingUsingPrevious / expectedPreviousDifficulty * 100, 2);
            log.info("Difficulty of previous network load deviates from the proofOfWork difficulty but is inside the tolerated range.\n" +
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedPreviousDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
            return false;
        }

        log.warn("Difficulties of current and previous network load deviate from the proofOfWork difficulty and are outside the tolerated range.\n" +
                        "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
        return true;
    }

    private byte[] getPayload(EnvelopePayloadMessage message) {
        return message.serialize();
    }

    private byte[] getChallenge(String peerAddress, int messageCounter) {
        return DigestUtil.sha256(ByteArrayUtils.concat(peerAddress.getBytes(Charsets.UTF_8),
                BigInteger.valueOf(messageCounter).toByteArray()));
    }

    private double calculateDifficulty(EnvelopePayloadMessage message, NetworkLoad networkLoad) {
        double messageCostFactor = MathUtils.bounded(MIN_MESSAGE_COST, 1, message.getCostFactor());
        double load = MathUtils.bounded(MIN_LOAD, 1, networkLoad.getLoad());
        double difficulty = TARGET_DIFFICULTY * messageCostFactor * load * networkLoad.getDifficultyAdjustmentFactor();
        return MathUtils.bounded(MIN_DIFFICULTY, MAX_DIFFICULTY, difficulty);
    }
}
