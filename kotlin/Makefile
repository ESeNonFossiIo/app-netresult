# Net Result: build, install, run and publish helpers (see ../kotlin-devkit/README.md)
PKG      := org.comichao.netresult
ACTIVITY := org.comichao.netresult.MainActivity

# Phone app and its Wear OS companion are released together.
MODULES := app wear

include ../kotlin-devkit/common.mk

## test: Run the unit tests
.PHONY: test
test:
	$(GRADLE) testDebugUnitTest
