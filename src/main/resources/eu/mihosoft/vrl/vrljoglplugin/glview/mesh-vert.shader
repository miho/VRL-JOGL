#version 150

in vec3 vertex_position;
in vec4 vertex_color;

uniform mat4 transform_matrix;
uniform mat4 view_matrix;

out vec3 ec_pos;
out vec4 v_col;

void main() {
    gl_Position = view_matrix * transform_matrix * vec4(vertex_position, 1.0);

    ec_pos = gl_Position.xyz;

    v_col = vertex_color;
}