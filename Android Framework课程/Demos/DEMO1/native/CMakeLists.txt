cmake_minimum_required(VERSION 3.13)
project(test1)

find_package(JNI REQUIRED)
include_directories(${JAVA_INCLUDE_PATH})
include_directories(${JAVA_INCLUDE_PATH2})

add_executable(test1 main.cpp)

message(INFO "jvm ${JAVA_JVM_LIBRARY}")

target_link_libraries(test1 ${JAVA_JVM_LIBRARY})