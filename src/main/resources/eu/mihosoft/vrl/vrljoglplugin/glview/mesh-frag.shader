#version 150

uniform float zoomInverse;

in vec3 ec_pos;

out vec4 fragColor;

void main() {

    // base colors
    // vec3 base3 = vec3(0.90, 0.98, 0.91);
    // vec3 base2 = vec3(0.98, 0.88, 0.82);
    // vec3 base00 = vec3(0.20, 0.38, 0.42);

    // vec3 base3 =  vec3(0.9,0.3,0.4);
    // vec3 base2 =  vec3(0.7,0.9,0.6);
    // vec3 base00 = vec3(0,0,0.2);

    vec3 base3 = vec3(0.90, 0.98, 0.91);
    vec3 base2 = vec3(0.98, 0.88, 0.82);
    vec3 base00 = vec3(0.18, 0.28, 0.34);

    // compute normal
    vec3 ec_normal = normalize(cross(dFdx(ec_pos), dFdy(ec_pos)));

    // rescale normal to prevent color change during zoom
    ec_normal.z *= zoomInverse;

    ec_normal = normalize(ec_normal);

    // interpolation points
    float a = dot(ec_normal, vec3(  0.0,   0.0, 1.0 ));
    float b = dot(ec_normal, vec3(-0.57, -0.57, 0.57));

    // final color interpolation
    fragColor = vec4((a*base2 + (1.0-a)*base00)*0.5 +
                        (b*base3 + (1.0-b)*base00)*0.5, 1.0);
}