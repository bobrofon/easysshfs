# SPDX-License-Identifier: MIT
cmake_minimum_required(VERSION 3.10.2)

# The project generates following files:
#   * ssh-static-${arch}
#   * sshfs-static-${arch}
project(sshfs-static LANGUAGES NONE)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/Buildroot.cmake")

set(BUILDROOT_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/buildroot")

include("Buildroot")

set(SSHFS_STATIC_SUPPORTED_ARCHS armeabi-v7a arm64-v8a x86 x86_64)
set(SSHFS_STATIC_EXECUTABLES ssh sshfs)

foreach(arch IN LISTS SSHFS_STATIC_SUPPORTED_ARCHS)
	set(buildroot_sshfs buildroot-sshfs-static-${arch})
	buildroot_target(${buildroot_sshfs}
		# buildroot_target does not support multiple OUTPUT files. So it is not possible to specify
		# 'ssh' and 'sshfs' file targets. TODO(bobrofon): make patch for Buildroot.cmake upstream.
		# 'os-release' file is specified because it is regenerated every buildroot_target run.
		OUTPUT target/etc/os-release
		CONFIG configs/sshfs-static-${arch}.config
	)

	foreach(exe IN LISTS SSHFS_STATIC_EXECUTABLES)
		set(exe_target ${exe}-static-${arch})
		set(exe_path ${buildroot_sshfs}/target/usr/bin/${exe})
		set(out_path "${CMAKE_CURRENT_BINARY_DIR}/${exe_target}")

		add_custom_command(
			OUTPUT "${out_path}"
			DEPENDS ${buildroot_sshfs}
			COMMAND cmake -E copy ${exe_path} "${out_path}"
		)
		# This target is required because targets added by add_custom_command cannot be used
		# in parents directory.
		add_custom_target(${exe_target}-target
			DEPENDS "${out_path}"
		)
	endforeach()
endforeach()
