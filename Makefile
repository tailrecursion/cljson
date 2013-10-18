
tests:
	lein with-profile testz cljsbuild clean
	lein with-profile testz cljsbuild test
	./test/run.sh

install:
	lein with-profile deployz install

push:
	./script/push
