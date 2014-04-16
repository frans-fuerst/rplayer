PROJECT( rplayer_qtclient_pc )

CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

FIND_PACKAGE( Qt4 REQUIRED )
#SET( QT_USE_QTXML TRUE)
SET( QT_USE_QTNETWORK TRUE)
INCLUDE( ${QT_USE_FILE} )

QT4_WRAP_UI( ${PROJECT_NAME}_UIS
    mainwindow.ui
)

# list all class files which implement Q_OBJECT
QT4_WRAP_CPP( ${PROJECT_NAME}_MOCS
    mainwindow.h
)

FILE( GLOB ${PROJECT_NAME}_files
    main.cpp
    mainwindow.cpp
    CMakeLists.txt
	*.h
)

INCLUDE_DIRECTORIES(
    ${CMAKE_CURRENT_BINARY_DIR}
#    ${CMAKE_CURRENT_SOURCE_DIR}
)

ADD_EXECUTABLE(
    ${PROJECT_NAME}
    ${${PROJECT_NAME}_files}
    ${${PROJECT_NAME}_MOCS}
    ${${PROJECT_NAME}_UIS}
)

TARGET_LINK_LIBRARIES(
    ${PROJECT_NAME}
    ${QT_LIBRARIES}
    ${ADDITIONAL_LIBRARIES}
)
