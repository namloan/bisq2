package bisq.chat;

import bisq.common.proto.NetworkProto;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Citation implements NetworkProto {
    public static final int MAX_TEXT_LENGTH = 1000;

    private final String authorUserProfileId;
    private final String text;

    public Citation(String authorUserProfileId, String text) {
        this.authorUserProfileId = authorUserProfileId;
        this.text = StringUtils.truncate(text, MAX_TEXT_LENGTH - 10);

        NetworkDataValidation.validateProfileId(authorUserProfileId);
        NetworkDataValidation.validateText(text, MAX_TEXT_LENGTH);
    }

    @Override
    public bisq.chat.protobuf.Citation toProto() {
        return bisq.chat.protobuf.Citation.newBuilder()
                .setAuthorUserProfileId(authorUserProfileId)
                .setText(text)
                .build();
    }

    public static Citation fromProto(bisq.chat.protobuf.Citation proto) {
        return new Citation(proto.getAuthorUserProfileId(),
                proto.getText());
    }

    public boolean isValid() {
        return authorUserProfileId != null && !authorUserProfileId.isEmpty() &&
                text != null && !text.isEmpty();
    }
}
