import React, { useState, useEffect, useReducer } from 'react';
var ReactNative = require('react-native');
var cljsBootstrap = require("./clojurescript-bootstrap.js");

function assert(predVal, message) {
  if(!predVal) {
	  throw new Error(message);
  }
}

function assertKeyType(obj, k, type) {
  assert(typeof obj[k] == type, k + " must be a " + type);
}

function validateOptions(options) {
  assertKeyType(options, "autoRefresh",    "boolean");
  assertKeyType(options, "renderFn",       "string");
  if(options.optionsUrl) {
	  assertKeyType(options, "optionsUrl", "string");
  } else {
	  assert(options["asset-path"], "must provide an asset-path option when no cljscOptionsUrl is provided");
	  assert(options["main"],       "must provide a main option when no cljscOptionsUrl is provided");
	  assertKeyType(options, "asset-path",      "string");
	  assertKeyType(options, "main",            "string");
	  if(options.preloads) {
	    assertKeyType(options, "preloads",        "string");
	  }
	  if(options["closure-defines"]) {
	    assertKeyType(options, "closure-defines", "string");
	  }
  }
}

function provideDefaultsAndValidateConfig(options){
  var config = Object.assign({renderFn: 'figwheel_rn_root',
				                      autoRefresh:  true},
			                       options);
  validateOptions(config);
  return config;
}

function cljsNamespaceToObject(ns) {
  return ns.replace(/\-/, "_").split(/\./).reduce(function (base, arg) {
	  return (base ? base[arg] : base)
  }, goog.global);
}

function listenForReload(cb) {
  if(cljsNamespaceToObject("figwheel.core.event_target")) {
	  figwheel.core.event_target.addEventListener("figwheel.after-load", cb);
  }
}

function FigwheelBridge(props) {
    const [state, updateState] = useState({loaded: false, root: null});
    const [, updateReload] = useReducer(function(accum, data){ return accum + data;}, 0);
    useEffect(function() {
      var refresh = function(e) {
        console.log("Refreshing Figwheel Root Element");
        updateReload(1);
	    }

      if (!state.loaded && (typeof goog === "undefined")) {
        loadApp(props.config, function (appRoot) {
		      goog.figwheelBridgeRefresh = refresh;
          updateState({loaded: true, root: appRoot});
		      if (props.config.autoRefresh) {
			      listenForReload(refresh);
		      }
        });
      }
    }, []);
    if (!state.root) {
      var plainStyle = {flex: 1, alignItems: 'center', justifyContent: 'center'};
      return (
          <ReactNative.View style={plainStyle}>
          <ReactNative.Text>Waiting for Figwheel to load files.</ReactNative.Text>
          </ReactNative.View>
      );
    }
  return state.root();
}

var createBridgeComponent = function(config) {
  var config = provideDefaultsAndValidateConfig(config);
  return function() {
    return React.createElement(FigwheelBridge, {config: config});
  };
}

function isChrome() {
  return typeof importScripts === "function"
}

var hostnameRegexp = /([^:]+:\/\/)([^:]+)(:.+)/;

function editHostname(url, hostname) {
  var parts = hostnameRegexp.exec(url);
  return [parts[1], hostname, parts[3]].join("");
}

// this is an odd bit to support the chrome debugger which is almost always
// local to the server
// this is a double usage of the url, probably better to explicit in the config
// to allow this behavior to be overriden
function correctUrl(url) {
  if(isChrome()) {
    return editHostname(url, "127.0.0.1");
  } else {
    return url;
  }
}

function loadApp(config, onLoadCb) {
  var confProm;
  if(config.optionsUrl) {
    confProm = cljsBootstrap.fetchConfig(correctUrl(config.optionsUrl)).then(function (conf) {
      return Object.assign(conf, config);
    }).catch(function(err){
      throw new Error("Figwheel Bridge Unable to fetch optionsUrl: " + config.optionsUrl, err);
	  });
  } else {
	  confProm = Promise.resolve(config);
  }
  if(confProm) {
    confProm.then(cljsBootstrap.bootstrap)
      .then(function (conf) {
        var mainNsObject = cljsNamespaceToObject(conf.main);
        assert(mainNsObject, "ClojureScript Namespace " + conf.main + " not found.");
        assert(mainNsObject[config.renderFn], "Render function " + config.renderFn + " not found.");
	      onLoadCb(function() { return mainNsObject[config.renderFn](); });
      }).catch(function(err){console.error(err)});
  }
}

// helper function to allow require at runtime
function shimRequire(requireMap) {
  // window get's compiled to the global object under React Native compile options
  var oldRequire = window.require;
  window.require = function (id) {
    var ret;
    if(ret = requireMap[id]) {
      return ret;
    }
    if(oldRequire) {
      return oldRequire(id);
	  }
  };
}

// deprecated
// this will not work when you use react native expo
// use createBridgeComponent instead
function startApp(options){
  assert(options.appName, "must provide an appName");
  assertKeyType(options, "appName", "string");
  // The crux of the loading problem for React Native is that the code needs to be loaded synchronously
  // because the way that React Native launches an application. It looks for the registered application to launch
  // after the initial loading of the jsbundle. Since we are accumstomed to use asynchronous loading to load
  // the optimizations none files and setup its useful to establish this fetching as a channel for future reloading.
  // We could compile the files to load into an initial single bundle to be loaded.
  ReactNative.AppRegistry.registerComponent(options.appName, () => createBridgeComponent(options));
}

module.exports = {
  shimRequire: shimRequire,
  start: startApp,
  createBridgeComponent: createBridgeComponent
};
