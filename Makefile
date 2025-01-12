build-ApiFunction:
	./gradlew clean nativeCompile
	cp ./build/native/nativeCompile/passhelper $(ARTIFACTS_DIR)
	cp ./build/resources/main/bootstrap $(ARTIFACTS_DIR)
	cp -r ./build/resources/main/templates $(ARTIFACTS_DIR)

build-NotificationFunction:
	./gradlew clean nativeCompile
	cp ./build/native/nativeCompile/passhelper $(ARTIFACTS_DIR)
	cp ./build/resources/main/bootstrap $(ARTIFACTS_DIR)
	cp -r ./build/resources/main/templates $(ARTIFACTS_DIR)