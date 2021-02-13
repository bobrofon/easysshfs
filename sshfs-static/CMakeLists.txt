cmake_minimum_required(VERSION 3.2)

project(sshfs-static LANGUAGES NONE)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/Buildroot.cmake")

set(BUILDROOT_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/buildroot")

include("Buildroot")

buildroot_target(sshfs-static-armeabi
	OUTPUT target/usr/bin/sshfs
	CONFIG configs/sshfs-static-armeabi.config
)
buildroot_target(sshfs-static-armeabi-v7a
	OUTPUT target/usr/bin/sshfs
	CONFIG configs/sshfs-static-armeabi-v7a.config
)
buildroot_target(sshfs-static-arm64-v8a
	OUTPUT target/usr/bin/sshfs
	CONFIG configs/sshfs-static-arm64-v8a.config
)
buildroot_target(sshfs-static-x86
	OUTPUT target/usr/bin/sshfs
	CONFIG configs/sshfs-static-x86.config
)
buildroot_target(sshfs-static-x86_64
	OUTPUT target/usr/bin/sshfs
	CONFIG configs/sshfs-static-x86_64.config
)

add_custom_target("${PROJECT_NAME}" ALL DEPENDS
	sshfs-static-armeabi
	sshfs-static-armeabi-v7a
	sshfs-static-arm64-v8a
	sshfs-static-x86
	sshfs-static-x86_64
)
