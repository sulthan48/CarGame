package com.example.renderer

import android.opengl.GLES20
import android.util.Log

object ShaderHelper {
    private const val TAG = "ShaderHelper"

    val VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        uniform vec3 uCameraPos;

        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec2 aTexCoord;
        attribute vec4 aColor;

        varying vec3 vNormal;
        varying vec3 vFragPos;
        varying vec2 vTexCoord;
        varying vec4 vColor;
        varying float vFogFactor;

        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vFragPos = worldPos.xyz;
            
            // Transform normal
            vNormal = normalize(mat3(uModelMatrix) * aNormal);
            vTexCoord = aTexCoord;
            vColor = aColor;

            // Fog calculation based on distance from camera
            float dist = distance(worldPos.xyz, uCameraPos);
            // Fog starts at 30m, fully obscure at 280m
            vFogFactor = clamp((dist - 30.0) / 250.0, 0.0, 1.0);

            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
        }
    """.trimIndent()

    val FRAGMENT_SHADER = """
        precision mediump float;

        uniform vec3 uLightDir;
        uniform vec3 uLightColor;
        uniform vec3 uAmbientColor;
        uniform vec3 uCameraPos;
        uniform vec3 uHeadlightPos;
        uniform vec3 uHeadlightDir;
        uniform float uHeadlightsEnabled;
        uniform float uIsWet;
        uniform vec3 uFogColor;
        uniform float uEmission;

        varying vec3 vNormal;
        varying vec3 vFragPos;
        varying vec2 vTexCoord;
        varying vec4 vColor;
        varying float vFogFactor;

        void main() {
            vec3 norm = normalize(vNormal);
            vec3 viewDir = normalize(uCameraPos - vFragPos);

            // Ambient
            vec3 ambient = uAmbientColor * vColor.rgb;

            // Directional Light (Sun / Moon)
            float diff = max(dot(norm, -uLightDir), 0.0);
            vec3 diffuse = diff * uLightColor * vColor.rgb;

            // Specular reflection (metallic car body / wet road)
            vec3 halfDir = normalize(-uLightDir + viewDir);
            float specPower = (uIsWet > 0.5 && norm.y > 0.8) ? 64.0 : 28.0;
            float spec = pow(max(dot(norm, halfDir), 0.0), specPower);
            vec3 specular = spec * (uLightColor * 0.7 + vec3(0.3)) * ((uIsWet > 0.5) ? 0.8 : 0.4);

            // Headlight Cone Spotlight (Projects forward at night)
            vec3 headlightContribution = vec3(0.0);
            if (uHeadlightsEnabled > 0.5) {
                vec3 lightToFrag = vFragPos - uHeadlightPos;
                float distToLight = length(lightToFrag);
                if (distToLight < 120.0) {
                    vec3 dirToFrag = normalize(lightToFrag);
                    float theta = dot(dirToFrag, normalize(uHeadlightDir));
                    float cutOff = 0.82; // ~35 degree cone
                    if (theta > cutOff) {
                        float intensity = clamp((theta - cutOff) / (1.0 - cutOff), 0.0, 1.0);
                        float attenuation = 1.0 / (1.0 + 0.04 * distToLight + 0.002 * distToLight * distToLight);
                        headlightContribution = vec3(1.0, 0.95, 0.85) * (intensity * attenuation * 3.5);
                    }
                }
            }

            vec3 litColor = ambient + diffuse + specular + (headlightContribution * vColor.rgb);

            // Emission (headlights/taillights glow without needing external light)
            if (uEmission > 0.1) {
                litColor = mix(litColor, vColor.rgb * 1.5, uEmission);
            }

            // Distance Fog
            vec3 finalColor = mix(litColor, uFogColor, vFogFactor);

            gl_FragColor = vec4(finalColor, vColor.a);
        }
    """.trimIndent()

    fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "Could not compile shader $type: $error")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)
        if (vertexShader == 0 || fragmentShader == 0) return 0

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, "Could not link program: $error")
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }
}
