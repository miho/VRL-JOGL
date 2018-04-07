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

    /**
     * Loads a mesh from the specified STL file (binary & ASCII supported) and deduplicates
     * the vertices after loading.
     *
     * @param file mesh file
     * @return mesh object
     * @throws IOException if an i/o error occurs during loading
     */
    public Mesh loadMesh(File file) throws IOException {

        System.out.println("-> loading mesh " + file);

        // parse STL file (binary or ascii)
        List<Vertex> vertexList = parse(file);
        
        return deduplicateTriangleVertices(vertexList);
    }

    /**
     * Deduplicates the soecified triangle vertices.
     *
     * @param vertexList vertices to deduplicate
     * @return mesh containing the deduplicated vertices and index list
     */
    public Mesh deduplicateTriangleVertices(List<Vertex> vertexList) {
        // init indices
        for (int i = 0; i < vertexList.size(); i++) {
            vertexList.get(i).index = i;
        }

        // in case of an empty file we just return an empty mesh object
        System.out.println("-> verts loaded " + vertexList.size());
        if (vertexList.isEmpty()) {
            System.out.println("-> empty mesh");
            return Mesh.newInstance(new float[0], new int[0]);
        }

        // start deduplication
        System.out.println("-> deduplicating verts");
        Vertex[] sortedVerts = new Vertex[vertexList.size()];
        sortedVerts = vertexList.toArray(sortedVerts);

        // sort vertices:
        // - duplicate vertices will be adjacent to each other
        //   if sortedVerts[i] != sortedVerts[i-1] then we know
        //   that it is unique among all vertices in the list
        // - parallel sort is done via fork-/join
        Arrays.parallelSort(sortedVerts, Vertex::compareVerts);

        System.out.println("-> sorted verts");

        // we create the index array (will be filled with indices below)
        int[] indices = new int[vertexList.size()];

        // we add each vertex once and filter out the duplicates
        // note: we use original vertex count as capacity to prevent
        //       unnecessary allocations & copying
        List<Vertex> newVerts = new ArrayList<>(vertexList.size());
        for (Vertex v : sortedVerts) {
            if (newVerts.isEmpty() // we can always add the first vertex (empty list)
                    || !v.equals(newVerts.get(newVerts.size() - 1))) {
                // or if the previous vertex is not equal to this one
                // see 'sort vertices:' above
                newVerts.add(v);
            }
            // set the index to the new location of vertex v
            indices[v.index] = newVerts.size() - 1;
        }

        // create final vertex array (flat float array)
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
        return Mesh.newInstance(finalVertices, indices);
    }

    /**
     * Simple vertex class. This class is only useful/tested
     * in the context of this loader.
     */
    private final static class Vertex {
        float x, y, z;
        int index;

        /**
         * Creates a new vertex.
         * @param x x coord
         * @param y y coord
         * @param z z coord
         */
        public Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {

            if (!(obj instanceof Vertex)) return false;

            Vertex other = (Vertex) obj;

            // we don't check for numerical equality because
            // in STL files duplicate vertices are exact clones
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

        /**
         * Compares the specified vertices.
         * @param v1 first vertex
         * @param v2 second vertex
         * @return {@code -1} for {@code v1 < v2}, {@code 0} for {@code v1 == v2} and {@code +1} for {@code v1 > v2}
         */
        static int compareVerts(Vertex v1, Vertex v2) {
            if (v1.x != v2.x) return Float.compare(v1.x, v2.x);
            else if (v1.y != v2.y) return Float.compare(v1.y, v2.y);
            else if (v1.z != v2.z) return Float.compare(v1.z, v2.z);
            else return 0;
        }
    }

    /**
     * Parses the specified STL file (binary and ASCII STL is supported).
     * @param f file to parse
     * @return list of vertices in this file
     * @throws IOException if an i/o error occurs during parsing
     */
    private List<Vertex> parse(File f) throws IOException {

        // determine if this is a binary or ASCII STL
        // and call either the binary or ascii parsing method

        // check whether the file is an ASCII STL
        if (isASCIISTLFile(f)) {
            System.out.println("-> ascii format detected");
            return parseAscii(f);
        }

        // the specified is no ASCII STL: we assume binary STL
        int numberOfTriangles = getNumberOfTriangles(f);
        if (isBinarySTLFile(f,numberOfTriangles)) {
            System.out.println("-> binary format detected");
            return parseBinary(f, numberOfTriangles);
        }

        throw new IOException("Unknown file format: " + f.getAbsolutePath());
    }

    /**
     * Indicates whether the specified file is an ASCII STL file.
     * @param f file to analyze
     * @return {@code true} if this file is an ASCII STL file; {@code false} otherwise
     * @throws IOException if an io error occurs
     */
    private boolean isASCIISTLFile(File f) throws IOException {
        try(BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            String[] words = line.trim().split("\\s+");
            return line.indexOf('\0') < 0 && words[0].equalsIgnoreCase("solid");
        } catch (IOException ex) {
            throw ex;
        }
    }

    /**
     * Indicates whether the specified file is a binary STL file.
     * @param f file to analyze
     * @return {@code true} if this file is a binary STL file; {@code false} otherwise
     * @throws IOException if an i/o error occurs
     */
    private boolean isBinarySTLFile(File f) throws IOException {
        return ((f.length() - 84) / 50) == getNumberOfTriangles(f);
    }

    /**
     * Indicates whether the specified file is a binary STL file.
     * @param f file to analyze
     * @param numberOfTriangles number of triangles in this file
     * @return {@code true} if this file is a binary STL file; {@code false} otherwise
     * @throws IOException if an i/o error occurs
     */
    private boolean isBinarySTLFile(File f, int numberOfTriangles) throws IOException {
        return ((f.length() - 84) / 50) == numberOfTriangles;
    }

    /**
     * Returns the number of triangles in the specified binary STL file.
     * @param f file to analyze
     * @return the number of triangles in the specified binary STL file
     * @throws IOException if an i/o error occurs
     */
    private int getNumberOfTriangles(File f) throws IOException {
        try(FileInputStream fs = new FileInputStream(f)) {

            // based on ImageJ/Fuji STL loader:
            //
            // bytes 80, 81, 82 and 83 form a little-endian int
            // that contains the number of triangles
            byte[] buffer = new byte[84];
            fs.read(buffer, 0, 84);
            return (int) (((buffer[83] & 0xff) << 24)
                    | ((buffer[82] & 0xff) << 16) | ((buffer[81] & 0xff) << 8) | (buffer[80] & 0xff));
        } catch(IOException ex) {
            throw ex;
        }
    }

    /**
     * Parses the specified ASCII STL file.
     * @param f file to parse
     * @return vertex list
     * @throws IOException if parsing fails
     */
    private List<Vertex> parseAscii(File f) throws IOException {
        List<Vertex> vertices = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] numbers = line.trim().split("\\s+");
                if (numbers[0].equals("vertex")) {
                    float x = Float.parseFloat(numbers[1]);
                    float y = Float.parseFloat(numbers[2]);
                    float z = Float.parseFloat(numbers[3]);

                    Vertex vector3f = new Vertex(x, y, z);
                    vertices.add(vector3f);
                } else if (numbers[0].equals("facet") && numbers[1].equals("normal")) {
                    // for now we ignore the normals
//                    normal.x = Float.parseFloat(numbers[2]);
//                    normal.y = Float.parseFloat(numbers[3]);
//                    normal.z = Float.parseFloat(numbers[4]);
                }
            }
        } catch (IOException e) {
            throw e;
        }

        return vertices;
    }

    /**
     * Parses the specified binary STL file.
     * @param f file to parse
     * @param numTriangles number of triangles to read
     * @return vertex list
     * @throws IOException if parsing fails
     */
    private List<Vertex> parseBinary(File f, int numTriangles) throws IOException {
        // initialize vertex list with the exact number of entries to prevent
        // unnecessary allocations & copying
        List<Vertex> vertices = new ArrayList<>(numTriangles*3);
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(f))) {

            // the following code is based on ImageJ/Fuji STL loader
            for (int h = 0; h < 84; h++) {
                fis.read();// skip the header bytes
            }

            // read triangles
            for (int t = 0; t < numTriangles; t++) {
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
        } catch (Exception e) {
            throw e;
        }

        return vertices;
    }

    private float leBytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        return Float.intBitsToFloat((((b3 & 0xff) << 24) | ((b2 & 0xff) << 16)
                | ((b1 & 0xff) << 8) | (b0 & 0xff)));
    }

}
