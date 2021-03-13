cmake_minimum_required(VERSION 3.2)
project(easysshfs LANGUAGES NONE)

add_subdirectory(sshfs-static)

set(EASYSSHFS_APP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/app)
set(EASYSSHFS_APK_DIR ${EASYSSHFS_APP_DIR}/build/outputs/apk)
file(GLOB_RECURSE EASYSSHFS_APP_SRC_FILES ${EASYSSHFS_APP_DIR}/src/*)

set(EASYSSHFS_SUPPORTED_ARCHS arm armv7a arm64v8a x86 x86_64)

set(EASYSSHFS_ABI_arm armeabi)
set(EASYSSHFS_ABI_armv7a armeabi-v7a)
set(EASYSSHFS_ABI_arm64v8a arm64-v8a)
set(EASYSSHFS_ABI_x86 x86)
set(EASYSSHFS_ABI_x86_64 x86_64)

set(EASYSSHFS_APK_arm Arm)
set(EASYSSHFS_APK_armv7a Armv7a)
set(EASYSSHFS_APK_arm64v8a Arm64v8a)
set(EASYSSHFS_APK_x86 X86)
set(EASYSSHFS_APK_x86_64 X86_64)

function(add_apk_target arch)
	set(apk_target ${PROJECT_NAME}-${arch})
	set(apk_path ${EASYSSHFS_APK_DIR}/${arch}/app-${arch}-unsigned.apk)
	set(src_dir ${EASYSSHFS_APP_DIR}/src/${arch})
	set(abi ${EASYSSHFS_ABI_${arch}})
	set(jni_dir ${src_dir}/${abi})
	set(libfake_path ${jni_dir}/libfake.so)
	set(assets_dir ${src_dir}/assets)
	set(ssh_asset_path ${assets_dir}/ssh)
	set(sshfs_asset_path ${assets_dir}/sshfs)
	set(bin_dir ${CMAKE_CURRENT_BINARY_DIR}/sshfs-static/sshfs-static-${abi}/target/usr/bin)
	set(ssh_bin_path ${bin_dir}/ssh)
	set(sshfs_bin_path ${bin_dir}/sshfs)

	add_custom_command(
		OUTPUT ${libfake_path} ${ssh_asset_path} ${sshfs_asset_path}
		DEPENDS sshfs-static-${abi}
		COMMAND mkdir -p ${jni_dir}
		COMMAND cmake -E touch ${libfake_path}
		COMMAND cmake -E copy ${ssh_bin_path} ${ssh_asset_path}
		COMMAND cmake -E copy ${sshfs_bin_path} ${sshfs_asset_path}
		COMMENT Creating native assets for ${arch}
		VERBATIM
	)
	add_custom_command(
		OUTPUT ${apk_path}
		DEPENDS ${EASYSSHFS_APP_SRC_FILES} ${libfake_path} ${ssh_asset_path} ${sshfs_asset_path}
		COMMAND ./gradlew assemble${EASYSSHFS_APK_${arch}}
		WORKING_DIRECTORY ${EASYSSHFS_APP_DIR}
		COMMENT Creating apk for ${arch}
		VERBATIM
	)

	add_custom_target(${apk_target}	DEPENDS ${apk_path})
endfunction()

add_custom_target(${PROJECT_NAME} ALL)

foreach(arch IN LISTS EASYSSHFS_SUPPORTED_ARCHS)
	add_apk_target(${arch})
	add_dependencies(${PROJECT_NAME} ${PROJECT_NAME}-${arch})
endforeach()
