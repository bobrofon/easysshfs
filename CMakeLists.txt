cmake_minimum_required(VERSION 3.0.0)
project(easysshfs LANGUAGES NONE)

add_subdirectory(sshfs-world)

file(GLOB_RECURSE SRC ${CMAKE_CURRENT_SOURCE_DIR}/app/src/*)

add_custom_command(
	OUTPUT
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/jniLibs/armeabi/libfake.so
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/assets/ssh
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/assets/sshfs
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/jniLibs/x86/libfake.so
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/assets/ssh
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/assets/sshfs
	DEPENDS sshfs-world
	COMMAND mkdir -p ${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/jniLibs/armeabi
	COMMAND mkdir -p ${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/jniLibs/x86
	COMMAND mkdir -p ${CMAKE_CURRENT_SOURCE_DIR}/sshfs-world/stable/arm
	COMMAND mkdir -p ${CMAKE_CURRENT_SOURCE_DIR}/sshfs-world/stable/i686
	COMMAND cmake -E touch ${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/jniLibs/armeabi/libfake.so
	COMMAND cmake -E touch ${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/jniLibs/x86/libfake.so
	COMMAND cmake -E copy ${CMAKE_CURRENT_SOURCE_DIR}/sshfs-world/stable/arm/ssh ${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/assets/ssh
	COMMAND cmake -E copy ${CMAKE_CURRENT_SOURCE_DIR}/sshfs-world/stable/arm/sshfs ${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/assets/sshfs
	COMMAND cmake -E copy ${CMAKE_CURRENT_SOURCE_DIR}/sshfs-world/stable/i686/ssh ${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/assets/ssh
	COMMAND cmake -E copy ${CMAKE_CURRENT_SOURCE_DIR}/sshfs-world/stable/i686/sshfs ${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/assets/sshfs
	VERBATIM
	)

add_custom_command(
	OUTPUT
		${CMAKE_CURRENT_SOURCE_DIR}/app/build/outputs/apk/arm/app-arm-unsigned.apk
		${CMAKE_CURRENT_SOURCE_DIR}/app/build/outputs/apk/x86/app-x86-unsigned.apk
	DEPENDS ${SRC} sshfs-world
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/jniLibs/armeabi/libfake.so
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/assets/ssh
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/arm/assets/sshfs
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/jniLibs/x86/libfake.so
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/assets/ssh
		${CMAKE_CURRENT_SOURCE_DIR}/app/src/x86/assets/sshfs
	COMMAND ./gradlew assembleArm assembleX86
	WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/app
	VERBATIM
	)

add_custom_target(
	${PROJECT_NAME} ALL
	DEPENDS
		${CMAKE_CURRENT_SOURCE_DIR}/app/build/outputs/apk/arm/app-arm-unsigned.apk
		${CMAKE_CURRENT_SOURCE_DIR}/app/build/outputs/apk/x86/app-x86-unsigned.apk
	)
