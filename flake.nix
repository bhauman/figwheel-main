{
  description = "My Ruby project with a development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11"; # Pin nixpkgs version here
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};

        # Create the bundler environment
        gems = pkgs.bundlerEnv {
          name = "my-ruby-project-env";
          ruby = pkgs.ruby_3_2;
          gemfile = ./Gemfile;
          lockfile = ./Gemfile.lock;
          gemset = ./gemset.nix;
        };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.ruby_3_2
            gems
            # Add other packages here, e.g.,
            # pkgs.nodejs
            # pkgs.yarn
          ];

          # Optional: Set environment variables or shell hooks
          # shellHook = ''
          #   export MY_VAR="some_value"
          #   echo "Welcome to the development environment!"
          # '';
        };
      }
    );
}
