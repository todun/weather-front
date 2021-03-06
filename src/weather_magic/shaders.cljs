(ns weather-magic.shaders
  (:require
   [thi.ng.geom.gl.core  :as gl]
   [thi.ng.glsl.core     :as glsl :include-macros true]
   [thi.ng.glsl.vertex   :as vertex]
   [thi.ng.glsl.lighting :as light]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.matrix   :as mat :refer [M44]]))

(def space-vs
  "void main() {
     vUV         = uv;
     vNormal     = normal;
     gl_Position = proj * view * model * vec4(position, 1.0);
   }")

(def space-fs
  "void main() {
     gl_FragColor = texture2D(starsTex, vec2(mod(uvLeftRightOffset + (uvOffset.x + vUV.x) / 2.0, 1.0), mod((uvOffset.y + vUV.y) / 2.0, 1.0))) + vec4(vNormal, 1.0) * 0.01;
   }")

(def standard-vs
  "void main() {
     vUV         = uv;
     vNormal     = normal;
     gl_Position = proj * view * model * vec4(position, 1.0);
   }")

(def standard-fs
  "void main() {
     float lam = lambert(surfaceNormal(vNormal, normalMat),
                         normalize(lightDir));

     vec4 baseTexture = texture2D(base, vUV);
     vec4 baseColor = vec4(ambientCol, 1.0) + baseTexture * vec4(lightCol, 1.0) * lam;
     gl_FragColor = baseColor;
   }")

(def temperature-fs
  "void main() {
     float lam = lambert(surfaceNormal(vNormal, normalMat),
                         normalize(lightDir));

     float temperature = texture2D(data, mod((vUV - dataPos), 1.0) / dataScale).r;
     vec4 baseTexture = texture2D(base, vUV);

     vec4 temperatureColor = vec4(0.0);
     float textureAlpha = texture2D(data, (vUV - dataPos) / dataScale).a;

     if (mod(temperature, 0.078125) < 0.003 && eye.z < 1.2) {
       if (temperature > 0.5) {
         temperatureColor = vec4(1.0, 1.0, 1.0, 1.0);
       } else if (temperature < 0.5) {
         temperatureColor = vec4(1.0, 1.0, 1.0, 1.0);
       }
     } else if(temperature > 0.72) {
       temperature = clamp(temperature, 0.72, 0.86);
       temperatureColor = vec4(clamp((0.86 - temperature) / 0.14, 0.0, 1.0),
                               0,
                               0.0, 1.0);
     } else if(temperature > 0.58) {
       temperatureColor = vec4(1.0,
                               (223.0 / 255.0) * (0.72 - temperature) / 0.14,
                               0.0, 1.0);
     } else if(temperature > 0.44) {
       temperatureColor = vec4((temperature - 0.44) / 0.14,
                               (223.0 / 255.0),
                               (0.58 - temperature) / 0.14, 1.0);
     } else if(temperature > 0.30){
       temperatureColor = vec4(0.0,
                               (223.0 / 255.0) * (temperature - 0.30) / 0.14,
                               (223.0 / 255.0) + (32.0 / 255.0) * (0.44 - temperature) / 0.14, 1.0);
     } else {
       temperature = clamp(temperature, 0.16, 0.30);
       temperatureColor = vec4((0.3 - temperature) / 0.14,
                               0.0,
                               clamp(1.0 + (116.0/255.0) * (temperature - 0.3) / 0.14, 0.0, 1.0), 1.0);
     }

     vec4 baseColor = vec4(ambientCol, 1.0) + baseTexture * vec4(lightCol, 1.0) * lam;
     temperatureColor = temperatureColor * textureAlpha;
     vec4 outColor;

     float baseFactor;
     float dataFactor;

     if(textureAlpha < 0.3) {
       baseFactor = 1.0;
       dataFactor = 0.0;
     } else {
       baseFactor = 0.35;
       dataFactor = 0.65;
     }

     vec4 mixColor = baseColor * baseFactor + temperatureColor * dataFactor;
     gl_FragColor = mixColor;
   }")

(def precipitation-fs
  "void main() {
     float lam = lambert(surfaceNormal(vNormal, normalMat),
                         normalize(lightDir));

     float precipitation = texture2D(data, mod((vUV - dataPos), 1.0) / dataScale).r;
     vec4 baseTexture = texture2D(base, vUV);

     vec4 precipitationColor = vec4(0.0, 0.0, 0.0, 0.0);
     float textureAlpha = texture2D(data, (vUV - dataPos) / dataScale).a;

     if(precipitation > 0.38) {
       float precipitationClamped = clamp(precipitation, 0.38, 0.63);
       precipitationColor = vec4(1.0,
                               0.0,
                               1.0 + (precipitationClamped - 0.63) / 0.25, 1.0);
     } else if(precipitation > 0.21) {
       precipitationColor = vec4(1.0,
                               (0.38 - precipitation) / 0.17,
                               0.0,
                               1.0);
     } else if(precipitation > 0.09) {
       precipitationColor = vec4(1.0 + (precipitation - 0.21) / 0.12,
                               1.0 + (32.0 / 255.0) * (precipitation - 0.21) / 0.12,
                               (223.0 / 255.0) * (0.21 - precipitation) / 0.12,
                               1.0);
     } else if(precipitation > 0.0) {
       precipitationColor = vec4(0.0,
                               (223.0 / 255.0) + (223.0 / 255.0) * (precipitation - 0.09) / 0.09,
                               (0.09 - precipitation) / 0.09,
                               1.0);
     } else {
       textureAlpha = 0.0;
     }

     vec4 baseColor = vec4(ambientCol, 1.0) + baseTexture * vec4(lightCol, 1.0) * lam;
     precipitationColor = precipitationColor * textureAlpha;
     vec4 outColor;

     float baseFactor;
     float dataFactor;

     if(textureAlpha < 0.3) {
       baseFactor = 1.0;
       dataFactor = 0.0;
     } else {
       baseFactor = 0.4;
       dataFactor = 0.6;
     }

     vec4 mixColor = baseColor * baseFactor + precipitationColor * dataFactor;
     gl_FragColor = mixColor;
   }")

(def standard-shader-spec
  {:vs standard-vs
   :fs (->> standard-fs
            (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
            (glsl/assemble))
   :uniforms {:model      [:mat4 M44]
              :view       :mat4
              :proj       :mat4
              :normalMat  [:mat4 (gl/auto-normal-matrix :model :view)]
              :base       [:sampler2D 1] ; Specify which texture unit
              :data       [:sampler2D 2] ; the uniform is bound to.
              :lightDir   [:vec3 [0.5 0.1 1]]
              :lightCol   [:vec3 [1 1 1]]
              :ambientCol [:vec3 [0 0 0]]
              :year       :float
              :range      :float
              :eye        :vec3
              :dataScale  :vec2
              :dataPos    :vec2}

   :attribs  {:position   :vec3
              :normal     :vec3
              :uv         :vec2}
   :varying  {:vUV        :vec2
              :vNormal    :vec3}
   :state    {:depth-test true}})

(def space-shader-spec
  {:vs space-vs
   :fs (->> space-fs
            (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
            (glsl/assemble))
   :uniforms {:model              [:mat4 M44]
              :view               :mat4
              :proj               :mat4
              :normalMat          [:mat4 (gl/auto-normal-matrix :model :view)]
              :starsTex           [:sampler2D 1]
              :uvLeftRightOffset  :float
              :uvOffset           :vec2}

   :attribs  {:position   :vec3
              :normal     :vec3
              :uv         :vec2}
   :varying  {:vUV        :vec2
              :vNormal    :vec3}
   :state    {:depth-test true}})

(def temperature-shader-spec
  (assoc standard-shader-spec
         :fs (->> temperature-fs
                  (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
                  (glsl/assemble))))

(def precipitation-shader-spec
  (assoc standard-shader-spec
         :fs (->> precipitation-fs
                  (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
                  (glsl/assemble))))
