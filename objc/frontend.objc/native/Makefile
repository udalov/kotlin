# ----------------------------------------------------------------------------
#  KotlinNativeIndexer Makefile
#
#  Usages:
#
#  1. make clean
#     Remove previously compiled files (out/ directory)
#
#  2. make proto
#     Launch Protocol Buffers compiler to generate C++ and Java classes
#
#  3. make
#     Compile everything and link to out/libKotlinNativeIndexer.dylib
#
#  4. make test
#     Compile (linking to the .dylib) and run tests
#
#  5. make javah
#     Launch javah on the compiled ObjCResolveFacade class (found in out/)
#     to produce a C header Indexer.h
#
# ----------------------------------------------------------------------------



# ----------------------------------------------------------------------------
#  Variables
# ----------------------------------------------------------------------------

PROJECT_NAME=KotlinNativeIndexer

# TODO: figure out why this doesn't work on Mavericks with JDK 7
# Worked on Mac OS X Lion/Mountain Lion with JDK 6
# Also see runtime.objc/native/make_native
# JAVA_INCLUDE=-I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers

JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/include
JAVA_INCLUDE=-I$(JAVA_HOME) -I$(JAVA_HOME)/darwin

CPP=c++
CC_FLAGS=-O2 -Wall -std=c++0x $(JAVA_INCLUDE) -Iinclude -stdlib=libstdc++
# libclang.dylib and libprotobuf.dylib should be installed via Homebrew:
# brew install llvm --disable-shared --universal --with-clang
# brew install protobuf
LD_FLAGS=-stdlib=libstdc++ -L/usr/local/opt/llvm/lib -L/usr/local/lib -lclang -lprotobuf

SRC_FILES=$(wildcard *.cc)
OBJ_FILES=$(patsubst %.cc,$(OUT)/%.o,$(SRC_FILES))
OUT=out
DYLIB=$(OUT)/lib$(PROJECT_NAME).dylib

TEST_FILES=$(wildcard tests/*.cc)
TEST_EXE=$(OUT)/tests
TEST_ENV=DYLD_LIBRARY_PATH=/usr/local/opt/llvm/lib


# ----------------------------------------------------------------------------
#  Targets
# ----------------------------------------------------------------------------

.PHONY: all proto mkdir dylib javah clean test


all: proto mkdir dylib

proto:
	@make -C proto all

mkdir:
	@mkdir -p $(OUT)

dylib: $(OBJ_FILES)
	$(CPP) $(LD_FLAGS) -dynamiclib -o $(DYLIB) $^

javah:
	@javah -classpath ../../../out/production/frontend.objc -o Indexer.h org.jetbrains.jet.lang.resolve.objc.ObjCResolveFacade

$(OUT)/%.o: %.cc
	$(CPP) $(CC_FLAGS) -c $^ -o $@



clean:
	@rm -rf $(OUT)



test: mkdir dylib
	$(CPP) $(CC_FLAGS) $(LD_FLAGS) -I. -L$(OUT) -l$(PROJECT_NAME) $(TEST_FILES) -o $(TEST_EXE)
	$(TEST_ENV) $(TEST_EXE)
