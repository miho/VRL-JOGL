/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.vrl.vrljoglplugin.glview;

import java.io.*;
import java.util.*;

/**
 * Very fast STL loader for binary and ASCII STL files.
 */
public class STLLoader {

    public STLLoader() {
        //
    }

    // private Point3f normal = new Point3f(0.0f, 0.0f, 0.0f); //to be used for file checking
    private int triangles;
    private String line;

    /**
     * Loads a mesh from the specified STL file (binary & ASCII supported) and deduplicates
     * the vertices after loading.
     *
     * @param file mesh file
     * @return mesh object
     * @throws IOException if loading failed
     */
    public Mesh loadMesh(File file) throws IOException {

        System.out.println("-> loading mesh " + file);

        List<Vertex> vertexList = parse(file);

        // init indices
        for (int i = 0; i < vertexList.size(); i++) {
            vertexList.get(i).index = i;
        }

        System.out.println("-> verts loaded " + vertexList.size());
        if (vertexList.isEmpty()) {
            System.out.println("-> empty mesh");
            return new Mesh(new float[0], new int[0]);
        }

        System.out.println("-> deduplicating verts");

        Vertex[] sortedVerts = new Vertex[vertexList.size()];
        sortedVerts = vertexList.toArray(sortedVerts);

        // sort vertices:
        // - duplicate vertices will be adjacent to each other
        //   if sortedVerts[i] != sortedVerts[i-1] then we know
        //   that it is unique among all vertices in the list

        Arrays.parallelSort(sortedVerts, Vertex::compareVerts);

        System.out.println("-> sorted verts");

        // we create the index array (will be filled with indices below)
        int[] indices = new int[vertexList.size()];

        // we add each vert once and filter out the duplicates
        List<Vertex> newVerts = new ArrayList<>();
        for (Vertex v : sortedVerts) {
            if (newVerts.isEmpty() // we can always add the first vertex (empty list)
                    || !v.equals(newVerts.get(newVerts.size() - 1))) {
                // or if the previous vertex is not equal to this one
                // see 'sort vertices:' above
                newVerts.add(v);
            }
            // set the index to the new location of v
            indices[v.index] = newVerts.size() - 1;
        }

        // create final float vertex array
        float[] finalVertices = new float[newVerts.size() * 3];
        for (int i = 0; i < newVerts.size(); i++) {
            finalVertices[i * 3 + 0] = newVerts.get(i).x;
            finalVertices[i * 3 + 1] = newVerts.get(i).y;
            finalVertices[i * 3 + 2] = newVerts.get(i).z;
        }

        System.out.println("-> deduplication finished");
        System.out.println("   #removed-verts:   " + (vertexList.size() - finalVertices.length));
        System.out.println("   #remaining-verts: " + newVerts.size());

        // finally return the mesh
        return new Mesh(finalVertices, indices);
    }

    /**
     * Simple vertex class. Equals method only works for copied vertices
     * without any numeric manipulation since equals will not work as
     * expected.
     */
    final static class Vertex {
        float x, y, z;
        int index;

        public Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {

            if (!(obj instanceof Vertex)) return false;

            Vertex other = (Vertex) obj;

            // we don't use numerical comparison because of
            // the specific duplication that occurs in this
            // file format
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }

        @Override
        public String toString() {
            return "Vertex {" +
                    "index=" + index +
                    ", x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }

        static int compareVerts(Vertex v1, Vertex v2) {

            if (v1.x != v2.x) return Float.compare(v1.x, v2.x);
            else if (v1.y != v2.y) return Float.compare(v1.y, v2.y);
            else if (v1.z != v2.z) return Float.compare(v1.z, v2.z);
            else return 0;
        }
    }


    private List<Vertex> parse(File f) throws IOException {
        // determine if this is a binary or ASCII STL
        // and send to the appropriate parsing method

        // Hypothesis 1: this is an ASCII STL
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        String[] words = line.trim().split("\\s+");
        if (line.indexOf('\0') < 0 && words[0].equalsIgnoreCase("solid")) {
            return parseAscii(f);
        }

        // Hypothesis 2: this is a binary STL
        FileInputStream fs = new FileInputStream(f);

        // bytes 80, 81, 82 and 83 form a little-endian int
        // that contains the number of triangles
        byte[] buffer = new byte[84];
        fs.read(buffer, 0, 84);
        triangles = (int) (((buffer[83] & 0xff) << 24)
                | ((buffer[82] & 0xff) << 16) | ((buffer[81] & 0xff) << 8) | (buffer[80] & 0xff));

        if (((f.length() - 84) / 50) == triangles) {
            return parseBinary(f);
        }

        System.err.println("WARNING: this file is invalid. Returning empty vertices anyway.");

        return Collections.emptyList();
    }

    private List<Vertex> parseAscii(File f) {
        List<Vertex> vertices = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
            vertices = new ArrayList<>();
            while ((line = in.readLine()) != null) {
                String[] numbers = line.trim().split("\\s+");
                if (numbers[0].equals("vertex")) {
                    float x = parseFloat(numbers[1]);
                    float y = parseFloat(numbers[2]);
                    float z = parseFloat(numbers[3]);

                    Vertex vector3f = new Vertex(x, y, z);
                    vertices.add(vector3f);
                } else if (numbers[0].equals("facet") && numbers[1].equals("normal")) {
                    // for now we ignore the normals
//                    normal.x = parseFloat(numbers[2]);
//                    normal.y = parseFloat(numbers[3]);
//                    normal.z = parseFloat(numbers[4]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return vertices;
    }

    private List<Vertex> parseBinary(File f) {
        List<Vertex> vertices = new ArrayList<>();
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(f))) {
            for (int h = 0; h < 84; h++) {
                fis.read();// skip the header bytes
            }
            for (int t = 0; t < triangles; t++) {
                byte[] tri = new byte[50];
                for (int tb = 0; tb < 50; tb++) {
                    tri[tb] = (byte) fis.read();
                }
//                normal.x = leBytesToFloat(tri[0], tri[1], tri[2], tri[3]);
//                normal.y = leBytesToFloat(tri[4], tri[5], tri[6], tri[7]);
//                normal.z = leBytesToFloat(tri[8], tri[9], tri[10], tri[11]);

                for (int i = 0; i < 3; i++) {
                    final int j = i * 12 + 12;
                    float x = leBytesToFloat(tri[j], tri[j + 1], tri[j + 2],
                            tri[j + 3]);
                    float y = leBytesToFloat(tri[j + 4], tri[j + 5],
                            tri[j + 6], tri[j + 7]);
                    float z = leBytesToFloat(tri[j + 8], tri[j + 9],
                            tri[j + 10], tri[j + 11]);

                    Vertex vector3f = new Vertex(x, y, z);
                    vertices.add(vector3f);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return vertices;
    }

    private float parseFloat(String string) {
        return Float.parseFloat(string);
    }

    private float leBytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        return Float.intBitsToFloat((((b3 & 0xff) << 24) | ((b2 & 0xff) << 16)
                | ((b1 & 0xff) << 8) | (b0 & 0xff)));
    }

}
