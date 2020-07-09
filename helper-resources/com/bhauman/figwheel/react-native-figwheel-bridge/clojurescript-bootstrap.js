function fetchConfig(cljscOptsJsonUrl) {
    var parts = cljscOptsJsonUrl.split("/");
    parts.pop();
    var fileBasePath = parts.join("/");
    return fetch(cljscOptsJsonUrl).then(function(response) {
	if(response.ok) {
	    return response.text();
	}}).then(function (text) {
	    var config = JSON.parse(text);
	    config["asset-path"] = fileBasePath;
	    return config;
	});
}

/*

Bootstrap a clojurescript application that has been compiled under
:optimizations :none.

This script requires a config object with:

"asset-path"       set to the location to load the assets from
"main"             set to the JS munged cljs namespace that represents the root of the application
"preloads"         set to an array of JS munged namespaces to be loaded before the main ns
"closure-defines"  set to an map of closure defines

"loadFn"           set to a function to import and eval a url-or-path, defaults to loadFile

*/
function bootstrap(config) {
    var evaluate = config["evaluate"] || eval;
    config.asyncImportChain = Promise.resolve(true);
    var responseText = function(response) { if(response.ok) {return response.text(); }}
    if(!config.loadFn) {
      config.loadFn = function(url) { return fetch(url).then(responseText).then(evaluate) };
    }
    config.closureImportScript = function (url, opt_src_text) {
	if(opt_src_text) {
	    evaluate(opt_src_text);
	} else if(url) {
	    config.asyncImportChain =
		config.asyncImportChain
		.then(function(a) { return config.loadFn(url); })
		.catch(console.error);
	}
	return true;
    }
    window.CLOSURE_NO_DEPS = true;
    return config.loadFn(config["asset-path"] + "/goog/base.js")
	.then(function (d) {
	    goog.basePath = config["asset-path"] + "/goog/";
	    if(config["closure-defines"]){
		goog.global.CLOSURE_UNCOMPILED_DEFINES = config["closure-defines"];
	    }
	    goog.global.CLOSURE_IMPORT_SCRIPT = config.closureImportScript;

	    // it makes sense to set this up here given as its a no-op without
	    // figwheel and is easily overriden
	    goog.global.FIGWHEEL_IMPORT_SCRIPT = function(uri, callback) {
		config.asyncImportChain = config.asyncImportChain.then(function (d) {
		    return config.loadFn(uri.toString())
		}).then(function(d){ callback(true) }).catch(function(err) { callback(false); });
	    };
	    return config;
	}).then(function(d) {
	    return config.loadFn(config["asset-path"] + "/goog/deps.js");
	}).then(function(d) {
	    return config.loadFn(config["asset-path"] + "/cljs_deps.js");
	}).then(function (d) {
	    if(config.preloads) {
		config.preloads.map(goog.require);
	    }
	    if(config.main) {
		goog.require(config.main);
	    }
	    return config.asyncImportChain.then(function (d) {return config;});
	}).catch(function (err) {console.error(err);})
}

module.exports = {fetchConfig: fetchConfig,
		  bootstrap: bootstrap};

// fetchConfig("/cljs-out/dev/cljsc_opts.json").then(bootstrap);
