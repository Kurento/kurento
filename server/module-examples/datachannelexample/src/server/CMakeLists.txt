include (CodeGenerator)

generate_code (
  MODELS ${CMAKE_CURRENT_SOURCE_DIR}/interface
  SERVER_STUB_DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/implementation/objects
)
