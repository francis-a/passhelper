build-ApiFunction:
	gradle clean nativeCompile
	cp ./build/native/nativeCompile/passhelper $(ARTIFACTS_DIR)
	cp ./build/resources/main/bootstrap $(ARTIFACTS_DIR)
	cp -r ./build/resources/main/static $(ARTIFACTS_DIR)
	cp -r ./build/resources/main/templates $(ARTIFACTS_DIR)

build-NotificationFunction:
	gradle clean nativeCompile
	cp ./build/native/nativeCompile/passhelper $(ARTIFACTS_DIR)
	cp ./build/resources/main/bootstrap $(ARTIFACTS_DIR)
	cp -r ./build/resources/main/templates $(ARTIFACTS_DIR)