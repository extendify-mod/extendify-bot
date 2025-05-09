{ stdenv
, gradle_8
, makeWrapper
, jdk11
, lib
, tree
}:
let
  self = stdenv.mkDerivation (final: {
    pname = "extendify-bot";
    version = "1.0";
    jarName = "${final.pname}-${final.version}.jar";

    src = ./.;

    nativeBuildInputs = [
      gradle_8
      makeWrapper
      tree
    ];

    mitmCache = gradle_8.fetchDeps {
      inherit (final) pname;
      pkg = self;
      data = ./deps.json;
    };
gradleBuildTask = "jar";
    # needed for macos
    __darwinAllowLocalNetworking = true;

    gradleFlags = [
      "-Dfile.encoding=utf-8"
    ];

    doCheck = true;

    installPhase = ''
      runHook preInstall

      mkdir -p $out/{bin,share/${final.pname}}
      tree build
      cp build/libs/${final.jarName} $out/share/${final.pname}/${final.jarName}

      makeWrapper ${jdk11}/bin/java $out/bin/${final.pname} \
          --add-flags "-jar $out/share/${final.pname}/${final.jarName}"

      runHook postInstall
    '';

    meta = {
      sourceProvenance = with lib.sourceTypes; [
        fromSource
        binaryBytecode
      ];
    };
  });
in
self
