# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: validation.proto
# Protobuf Python Version: 4.25.0
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
from google.protobuf.internal import builder as _builder
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x10validation.proto\x12\x02\x64q\"\xb7\x01\n\nValidation\x12\n\n\x02ts\x18\x01 \x01(\x04\x12\x11\n\tvalidator\x18\x02 \x01(\t\x12\x0c\n\x04type\x18\x03 \x01(\t\x12\x0f\n\x07\x66\x65\x61ture\x18\x04 \x01(\t\x12%\n\x06result\x18\x05 \x01(\x0e\x32\x15.dq.Validation.Result\x12\x10\n\x08optional\x18\x06 \x01(\x08\x12\x13\n\x0b\x64\x65scription\x18\x07 \x01(\t\"\x1d\n\x06Result\x12\t\n\x05VALID\x10\x00\x12\x08\n\x04\x46\x41IL\x10\x01\x42\x35\n com.linksfoundation.dq.api.modelB\x0fValidationProtoP\x01\x62\x06proto3')

_globals = globals()
_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, _globals)
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, 'validation_pb2', _globals)
if _descriptor._USE_C_DESCRIPTORS == False:
  _globals['DESCRIPTOR']._options = None
  _globals['DESCRIPTOR']._serialized_options = b'\n com.linksfoundation.dq.api.modelB\017ValidationProtoP\001'
  _globals['_VALIDATION']._serialized_start=25
  _globals['_VALIDATION']._serialized_end=208
  _globals['_VALIDATION_RESULT']._serialized_start=179
  _globals['_VALIDATION_RESULT']._serialized_end=208
# @@protoc_insertion_point(module_scope)
