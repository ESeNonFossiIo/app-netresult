# Forward every target to the Gradle project in kotlin/.
.DEFAULT_GOAL := help
%:
	@$(MAKE) -C kotlin $@
