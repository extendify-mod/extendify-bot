{ stdenv
, gradle
, makeWrapper
, jdk11
, lib
,
}:
let
  self = stdenv.mkDerivation (final: {
    pname = "extendifybot";
    version = "1.0";
    jarName = "${final.pname}.jar";

    src = ./.;

    nativeBuildInputs = [
      gradle
      makeWrapper
    ];

    mitmCache = gradle.fetchDeps {
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
      cp build/libs/*.jar $out/share/${final.pname}/${final.jarName}

      makeWrapper ${jdk11}/bin/java $out/bin/${final.pname} \
          --add-flags "-jar $out/share/${final.pname}/${final.jarName}"

      runHook postInstall
    '';

    meta = {
      sourceProvenance = with lib.sourceTypes; [
        fromSource
        binaryBytecode
      ];
      mainProgram = "extendify-bot";
    };
  });
in
self
