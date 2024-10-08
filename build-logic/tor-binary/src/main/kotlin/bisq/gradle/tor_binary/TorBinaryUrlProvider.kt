package bisq.gradle.tor_binary

import bisq.gradle.tasks.PerOsUrlProvider

class TorBinaryUrlProvider(private val version: String) : PerOsUrlProvider {
    override val urlPrefix: String
        get() = "https://archive.torproject.org/tor-package-archive/torbrowser/$version/"

    override val linuxUrl: String
        get() = "tor-expert-bundle-linux-x86_64-$version.tar.gz"

    override val macOsUrl: String
        get() = "tor-expert-bundle-macos-x86_64-$version.tar.gz"

    override val windowsUrl: String
        get() = "tor-expert-bundle-windows-x86_64-$version.tar.gz"
}