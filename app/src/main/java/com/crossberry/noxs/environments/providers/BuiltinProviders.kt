/*
 * Noxs — original implementation.
 * Builtin environment providers (spec §2-§8 of the Kali Linux-first spec).
 *
 * Kali Linux (NetHunter Rootless) is the one and only environment for fresh
 * installs: installed through the proven NoxsInstaller pipeline (staged
 * downloads, TarGuard extraction, signed APT bootstrap) from the pinned,
 * official kali.download kali-base ARM64 artifact. There is no
 * distribution selector and no Debian fallback — the installer downloads and
 * installs ONLY the genuine Kali Linux (NetHunter Rootless) ARM64 rootfs.
 *
 * Existing (pre-migration) Debian installations of upgrading users are NOT
 * providers: they are imported read-only as a legacy environment by
 * NoxsEnvironmentManager (spec §9 — never silently erased, user data kept).
 */
package com.crossberry.noxs.environments.providers

import com.crossberry.noxs.environments.DeviceProfile
import com.crossberry.noxs.environments.EnvironmentProvider
import com.crossberry.noxs.environments.model.EnvironmentCapability
import com.crossberry.noxs.environments.model.CompatibilityReport
import com.crossberry.noxs.environments.model.EnvironmentVariant

// ------------------------------------------------------------------ Kali Linux

/**
 * Kali Linux (NetHunter Rootless) — the one Noxs environment (Kali Linux-first
 * spec). Installation runs the proven NoxsInstaller pipeline (staged
 * downloads, TarGuard extraction, signed APT bootstrap) against the official
 * Kali NetHunter Rootless minimal rootfs artifact, then configures the official
 * Kali rolling repository (http.kali.org/kali for all architectures)
 * and the `noxs@kali:~` terminal identity.
 * Noxs — original implementation.
 * Builtin environment providers (spec §2-§8 of the Kali Linux-first spec).
 *
 * Kali Linux (NetHunter Rootless) is the one and only environment for fresh
 * installs: installed through the proven NoxsInstaller pipeline (staged
 * downloads, TarGuard extraction, signed APT bootstrap) from the pinned,
 * official kali.download kali-base ARM64 artifact. There is no
 * distribution selector and no Debian fallback — the installer downloads and
 * installs ONLY the genuine Kali Linux (NetHunter Rootless) ARM64 rootfs.
 *
 * Existing (pre-migration) Debian installations of upgrading users are NOT
 * providers: they are imported read-only as a legacy environment by
 * NoxsEnvironmentManager (spec §9 — never silently erased, user data kept).
 */
package com.crossberry.noxs.environments.providers

import com.crossberry.noxs.environments.DeviceProfile
import com.crossberry.noxs.environments.EnvironmentProvider
import com.crossberry.noxs.environments.model.EnvironmentCapability
import com.crossberry.noxs.environments.model.CompatibilityReport
import com.crossberry.noxs.environments.model.EnvironmentVariant

// ------------------------------------------------------------------ Kali Linux

/**
 * Kali Linux (NetHunter Rootless) — the one Noxs environment (Kali Linux-first
 * spec). Installation runs the proven NoxsInstaller pipeline (staged
 * downloads, TarGuard extraction, signed APT bootstrap) against the official
 * Kali NetHunter Rootless minimal rootfs artifact, then configures the official
 * Kali rolling repository (http.kali.org/kali for all architectures)
 *  terminal identity.
 */
class KaliProvider : EnvironmentProvider {
    override val id = "kali"
    override val displayName = "Kali Linux (NetHunter Rootless)"
    override val description = "Genuine Kali Linux (NetHunter Rootless) userspace"
    override val recommended = true

    override suspend fun runInstall(ctx: com.crossberry.noxs.environments.InstallContext,
                                    variant: EnvironmentVariant): Boolean {
        val installer = com.crossberry.noxs.runtime.NoxsInstaller(
            ctx.context, ctx.paths, ctx.launcher, ctx.isCancelled
        ).apply {
            promptHostLabel = "kali"
            distroBanner = "Noxs Kali Linux (kali-rolling)"
        }
        val progress = object : com.crossberry.noxs.runtime.NoxsInstaller.Progress {
            override fun onStep(step: Int, titleRes: Int, detail: String) {
                val state = when (step) {
                    5 -> com.crossberry.noxs.environments.model.SetupState.DOWNLOADING
                    6 -> com.crossberry.noxs.environments.model.SetupState.EXTRACTING
                    7, 9 -> com.crossberry.noxs.environments.model.SetupState.CONFIGURING
                    8 -> com.crossberry.noxs.environments.model.SetupState.CREATING_USER
                    10 -> com.crossberry.noxs.environments.model.SetupState.VERIFYING_ENVIRONMENT
                    else -> com.crossberry.noxs.environments.model.SetupState.PREPARING
                }
                val op = detail.ifBlank { ctx.context.getString(titleRes) }
                ctx.onStage(state, op)
            }
            override fun onProgressBytes(downloaded: Long, total: Long) =
                ctx.onDownloadProgress(downloaded, total)
            override fun onExtracted(entries: Long) = ctx.onExtractProgress(entries)
            override fun onLog(line: String) = ctx.onLog(line)
            override fun onPasswordRequired(): CharArray? = ctx.passwordProvider()
        }
        return when (val result = installer.install(progress)) {
            is com.crossberry.noxs.runtime.NoxsInstaller.InstallResult.Success -> true
            is com.crossberry.noxs.runtime.NoxsInstaller.InstallResult.Failure ->
                throw java.io.IOException(
                    ctx.context.getString(result.userMessageRes) + ": " + result.detail
                )
            com.crossberry.noxs.runtime.NoxsInstaller.InstallResult.Cancelled ->
                throw com.crossberry.noxs.environments.SetupCancelledException()
        }
    }

    override fun capabilities() = setOf(
        EnvironmentCapability.TERMINAL, EnvironmentCapability.FILES,
        EnvironmentCapability.PROCESSES, EnvironmentCapability.PACKAGE_MANAGER,
        EnvironmentCapability.SUDO, EnvironmentCapability.DOCKER,
        EnvironmentCapability.CODE_SERVER, EnvironmentCapability.BACKGROUND_TASKS
    )

    override fun variants(): List<EnvironmentVariant> = listOf(
        EnvironmentVariant(
            id = "kali-rootless",
            name = "Kali NetHunter Rootless (minimal)",
            description = "Official Kali NetHunter Rootless minimal userspace for ARM64, ARMHF, and x86_64",
            downloadBytesEstimate = 125_829_120L,
            extractedBytesEstimate = 900_000_000L,
            isDefault = true
        )
    )

    override fun checkCompatibility(device: DeviceProfile): CompatibilityReport =
        com.crossberry.noxs.environments.CompatibilityEngine.report(
            device = device,
            requiredStorageBytes = StorageRequirements.forVariant(125_829_120L, 900_000_000L)
        )
}

// NOTE: Noxs ships exactly ONE environment — Kali Linux (NetHunter Rootless) (kali-rolling). There
// is no Debian/Kali Linux selector and no Debian fallback: the install pipeline
// downloads the pinned official Kali Linux base rootfs only. Legacy Debian
// installs of upgrading users are imported by NoxsEnvironmentManager as a
// read-only migration source (spec §9) — they are not providers.

// ------------------------------------------------------------------ registry

/** Central provider discovery (spec §2, §57). */
object ProviderRegistry {

    fun createAll(termuxInstalled: Boolean): List<EnvironmentProvider> = listOf(
        KaliProvider()
    )

    fun findById(providers: List<EnvironmentProvider>, id: String): EnvironmentProvider? =
        providers.firstOrNull { it.id == id }
}

object StorageRequirements {
    fun forVariant(downloadBytes: Long, extractedBytes: Long): Long =
        com.crossberry.noxs.environments.model.StoragePlan.compute(downloadBytes, extractedBytes).totalRequired
}
