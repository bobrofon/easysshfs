# SPDX-License-Identifier: MIT
cmake_minimum_required(VERSION 3.10.2)

# The project generates following files:
#   * assets/${ANDROID_ABI}/ssh
#   * assets/${ANDROID_ABI}/sshfs
#   * lib/${ANDROID_ABI}/libstub.so
project(assets LANGUAGES NONE)

# Usually 'CMAKE_CURRENT_BINARY_DIR' is something like '.cxx/cmake/debug/${ANDROID_ABI}'.
# Let 'SSHFS_BINARY_DIR' to be shared between all 'ANDROID_ABI'.
# Instead of multiple '.cxx/cmake/debug/${ANDROID_ABI}/sshfs-static' folders
# the only one '.cxx/cmake/debug/sshfs-static' folder will be created.
# The main reason of that is to reuse 'BR2_DL_DIR' cache between all 'ANDROID_ABI'.
set(SSHFS_BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/../sshfs-static")
# import 'ssh-static-${ANDROID_ABI}-target' and 'sshfs-static-${ANDROID_ABI}-target' targets
add_subdirectory(../sshfs-static "${SSHFS_BINARY_DIR}")
# import 'stub' library
add_subdirectory(src/main/cpp)

set(ASSETS_PATH "${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/${ANDROID_ABI}")
set(ASSETS_EXECUTABLES ssh sshfs)

foreach(exe IN LISTS ASSETS_EXECUTABLES)
    set(src_path "${SSHFS_BINARY_DIR}/${exe}-static-${ANDROID_ABI}")
    set(dst_path "${ASSETS_PATH}/${exe}")
    add_custom_command(
        OUTPUT "${dst_path}"
        DEPENDS ${exe}-static-${ANDROID_ABI}-target
        COMMAND cmake -E copy "${src_path}" "${dst_path}"
        VERBATIM
    )
    add_custom_target("${exe}-target"
        DEPENDS "${dst_path}"
        VERBATIM
    )
    # 'stub' is just a dummy library and there are no actual dependencies between 'stub' and assets
    # files. But because of lack of CMake support in Android build system there is no way to tell
    # Gradle to build custom CMake targets as an externalNativeBuild. So the only way to build the
    # assets with CMake is to make them the dependencies of the 'stub' library.
    add_dependencies(stub "${exe}-target")
endforeach()
