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

# Working path on Mac OS X Lion/Mountain Lion with JDK 6:
# JAVA_INCLUDE=-I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers
JAVA_HOME=$(shell /usr/libexec/java_home)
JAVA_INCLUDE=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin

CXX=c++
CXXFLAGS=-O2 -Wall -std=c++0x $(JAVA_INCLUDE) -Iinclude -stdlib=libc++
# libclang.dylib and libprotobuf.dylib should be installed via Homebrew:
# brew install llvm --disable-shared --universal --with-clang --without-python
# brew install protobuf
LDFLAGS=-stdlib=libc++ -L/usr/local/opt/llvm/lib -L/usr/local/opt/protobuf/lib -lclang -lprotobuf

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
	$(CXX) $(LDFLAGS) -dynamiclib -o $(DYLIB) $^

javah:
	@javah -classpath ../../../out/production/frontend.objc:../../../out/production/descriptors:../../../dependencies/bootstrap-compiler/Kotlin/lib/kotlin-runtime.jar:../../../ideaSDK/core/intellij-core.jar -o Indexer.h org.jetbrains.jet.lang.resolve.objc.ObjCPackageFragmentProvider

$(OUT)/%.o: %.cc
	$(CXX) $(CXXFLAGS) -c $^ -o $@



clean:
	@rm -rf $(OUT)



test: mkdir dylib
	$(CXX) $(CXXFLAGS) $(LDFLAGS) -I. -L$(OUT) -l$(PROJECT_NAME) $(TEST_FILES) -o $(TEST_EXE)
	$(TEST_ENV) $(TEST_EXE)
