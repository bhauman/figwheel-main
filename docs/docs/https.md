---
title: HTTPS
layout: docs
category: docs
order: 13
---

# HTTPS

<div class="lead-in">The web is becoming more insistent on using HTTPS
to ensure secure communications between client and server. While still
rare, there are some situations where it is very beneficial to use
HTTPS in your development environment.</div>

> Figwheel is currently working to make using HTTPS locally as easy as
> setting [`:use-ssl`](/config-options#use-ssl) to `true` in your
> Figwheel Options. The support for this is strong on MacOS and
> Linux. Support on Windows however still needs some work.

It's important to remember that you may not need the figwheel server
to use HTTPS. If you are serving your application from [your own
server](/docs/your_own_server) (something you are going to need to do
eventually) and that server is using HTTPS then connecting to a local
websocket (for Figwheel) should not require an SSL connection. Another
thing to note is that connections to `127.0.0.1` rather than
`localhost` do not require an SSL connection either.

If you are serving your application via the Figwheel server and decide
you want to use HTTPS, Figwheel tries to make this as easy as possible.

You can use [`:use-ssl true`](#use-ssl-syntactic-sugar-and-automation)
to help you configure the Figwheel server to use SSL but let's look at
the manual configuration first.

## Manual configuration

We'll start with a basic Figwheel Main configuration in `dev.cljs.edn` like:

```clj
{:main example.core}
```

### Configure Jetty

We'll want to add the SSL configuration to `:ring-server-options`:

```clj
^{:ring-server-options {:ssl? true
                        :ssl-port 9533
                        :keystore <path-to-java-key-store>
                        :key-password <password-for-key-store>}}
{:main example.core}
```

These options tell the `ring.jetty.adapter` to use SSL. You'll have to
replace `<path-to-java-key-store>` and `<password-for-key-store>` with
the actual values from our own Java KeyStore.

A Java KeyStore is a bundle of certificates and keys. The keys are
needed to sign server responses so that clients know that they are
valid.

#### Using `certifiable` to create a keystore

If you are not on the Windows platform you can use
[certifiable](https://github.com/bhauman/certifiable) to create a Java
key store. 

[Certifiable](https://github.com/bhauman/certifiable) is designed
create a safe certificate for local use. On the MacOS platform it also
adds trust for that certificate so that your browsers don't complain
about it. Its main advantage is that it works with tools that are
already installed if you are running Clojure. It is also very safe to
use.

[Certifiable](https://github.com/bhauman/certifiable) is a dependency
of Figwheel so you can call it easily on the command line. 

Make sure you add any `-A` aliases needed to include the
`com.bhauman/figwheel-main` dependency and call:

```shell
$ clj -m certifiable.main 
```

This will generate a certificate for you and finally output something
like this:

```shell
--------------------------- Setup Instructions ---------------------------
Local dev Java keystore generated at: /Users/bhauman/_certifiable_certs/localhost-1d070e4/dev-server.jks
The keystore type is: "JKS"
The keystore password is: "password"
The root certificate is: /Users/bhauman/_certifiable_certs/localhost-1d070e4/dev-root-trust-this.pem
For System: root certificate is trusted
For Firefox: root certificate is trusted
Example SSL Configuration for ring.jetty.adapter/run-jetty:
{:ssl? true,
 :ssl-port 9533,
 :keystore "/Users/bhauman/_certifiable_certs/localhost-1d070e4/dev-server.jks",
 :key-password "password"}
```

The last few lines of the output is an example of the configuration
you need to pass to `:ring-server-options`.

So given the above certifiable command we can finish the
`:ring-server-options` config in our `dev.cljs.edn` like so:

```clj
^{:ring-server-options {:ssl? true
                        :ssl-port 9533
                        :keystore "/Users/bhauman/_certifiable_certs/localhost-1d070e4/dev-server.jks"
                        :key-password "password"}}
{:main example.core}
```

#### Using `mkcert` to create one

Another option is the
[`mkcert`](https://github.com/FiloSottile/mkcert) tool.

[Mkcert](https://github.com/FiloSottile/mkcert) will create and set up
 trust for a certificate as well.
 
 Follow the instructions to install [`mkcert`](https://github.com/FiloSottile/mkcert). (i.e. `brew install mkcert` on MacOS).
 
 You can then ensure that its root certificate is installed and
 trusted by running:
 
```shell
 $ mkcert -install
```

To create a keystore that Jetty will understand, we will instruct
`mkcert` to generate a `PKCS#12` bundle.

You can create a keystore for `localhost` by running:

```shell
$ mkcert -pkcs12 localhost
```

This will output a `localhost.p12` file in your current directory.

We can utilize this certificate/key bundle in our config like so:

```clj
^{:ring-server-options {:ssl? true
                        :ssl-port 9533
                        :keystore "localhost.p12" 
                        :keystore-type "PKCS12" ;; <- IMPORTANT!
                        :key-password "changeit"}}
{:main example.core}
```

Be sure to indicate the keystore type is `PKCS12`.

### Configure `:connect-url` and `:open-url`

You'll want the connection to Figwheel to go over SSL so we'll need
to change the [`:connect-url`](/config-options#connect-url) to
`"wss://[[config-hostname]]:9533/figwheel-connect"`

You'll also want the browser that pops open after the build starts to
be an `https` URL as well. You can do this by setting
[`:open-url`](/config-options#open-url) to `"https://[[server-hostname]]:9533"`

It's important to remember that both `[[config-hostname]]` and
`[[server-hostname]]` are template variables that are filled in by
Figwheel.

### Complete manual config

So our finished configuration will something like:

```clj
^{:connect-url "wss://[[config-hostname]]:9533/figwheel-connect"
  :open-url "https://[[server-hostname]]:9533"
  :ring-server-options {:ssl? true
                        :ssl-port 9533
                        :keystore "/Users/bhauman/_certifiable_certs/localhost-1d070e4/dev-server.jks"
                        :key-password "password"}}
{:main example.core}
```

## `:use-ssl` syntactic sugar and automation

`:use-ssl` attempts to set all of the above options for you
automatically. It will try to use
[Certifiable](https://github.com/bhauman/certifiable) to create or
reuse an existing certificate for localhost if neither `:keystore` or
`:truststore` are set already.

So you could just set `:use-ssl` to `true` like so:

```clj
^{:use-ssl true}
{:main example.core}
```

And that may work, if you need to supply your own certificate and you
can easily do that like so:

```clj
^{:ring-server-options {:keystore "/Users/bhauman/_certifiable_certs/localhost-1d070e4/dev-server.jks"
                        :key-password "password"}}
{:main example.core}
```

> `:use-ssl` will not override any configuration that you provide.

## Conclusion

The above should get you up and running will SSL and Figwheel. You
should rely on `:use-ssl` when possible.



