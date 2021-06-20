cmake_minimum_required(VERSION 3.10.2)

project(stub LANGUAGES CXX)

add_library(${PROJECT_NAME} SHARED stub.cpp)
