# SPDX-License-Identifier: MIT
cmake_minimum_required(VERSION 3.10.2)

# The project generates following files:
#   * lib/${ANDROID_ABI}/libssh.so
#   * lib/${ANDROID_ABI}/libsshfs.so
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
# import 'stub' library. externalNativeBuild requires sume sources to generate compile_commands.json
# TODO(bobrofon): report issue
add_subdirectory(src/main/cpp)

set(ASSETS_EXECUTABLES ssh sshfs)

foreach(exe IN LISTS ASSETS_EXECUTABLES)
    set(src_path "${SSHFS_BINARY_DIR}/${exe}-static-${ANDROID_ABI}")
    # By default only 'lib*.so' files are packaging.
    set(dst_path "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/lib${exe}.so")
    add_custom_command(
        OUTPUT "${dst_path}"
        DEPENDS ${exe}-static-${ANDROID_ABI}-target
        COMMAND cmake -E copy "${src_path}" "${dst_path}"
        VERBATIM
    )
    add_custom_target(${exe}
        DEPENDS "${dst_path}"
        VERBATIM
    )
endforeach()
