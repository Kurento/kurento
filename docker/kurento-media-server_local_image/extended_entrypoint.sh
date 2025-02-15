#!/bin/bash


sysctl -w kernel.core_pattern=/coredump/core.%p

/entrypoint.sh
