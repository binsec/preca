/******************************************************************************/
/*  This file is part of PRECA.                                               */
/*  PRECA is part of the BINSEC toolbox for binary-level program analysis.    */
/*                                                                            */
/*  Copyright (C) 2019-2026                                                   */
/*  CEA (Commissariat à l'énergie atomique et aux énergies                    */
/*       alternatives)                                                        */
/*                                                                            */
/*  you can redistribute it and/or modify it under the terms of the GNU       */
/*  Lesser General Public License as published by the Free Software           */
/*  Foundation, version 2.1.                                                  */
/*                                                                            */
/*  It is distributed in the hope that it will be useful,                     */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of            */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             */
/*  GNU Lesser General Public License for more details.                       */
/*                                                                            */
/*  See the GNU Lesser General Public License version 2.1                     */
/*  for more details (enclosed in the file licenses/LGPLv2.1).                */
/*                                                                            */
/******************************************************************************/

package io.github.binsec.core.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;


import io.github.binsec.core.acqvariable.CellType;





public class Binsec {

    String binpath;
    String binsec_init;
    Integer emultimeout = 60;
    CellType[] types;
    boolean[] globals;
    Integer[] addrs_global;
    int last_offset=0;
    ArchType arch;
    int arch_size=0;

    String[] calling_convention_x86_64 = new String[]{
            "rdi",
            "rsi",
            "rdx",
            "rcx",
            "r8",
            "r9"
            //"@[rsp+8, 8]",
            //"@[rsp+16, 8]",
            //"@[rsp+24, 8]",
    };

    String[] calling_convention_arm32 = new String[]{
            "r0",
            "r1",
            "r2",
            "r3",
    };



    private void reset_last_offset() {
	    	switch(arch){
	        case ARM32: this.last_offset=-4; break;
	        case X86_64: this.last_offset=0; break;
	        default: assert false;
	    	}
    	}

    String[] binsec_command(File temp){
        if(arch==ArchType.ARM32){
            return new String[]{
                    "binsec",
                    "-sse",
                    "-sse-engine", "concrete",
                    "-sse-script", temp.getPath(),
                    binpath,
                    "-emul-perm", String.format("%s/resources/permissions%02d.config", System.getenv("PRECA_PATH"), arch_size),
                    "-sse-timeout", emultimeout.toString(),
                    "-sse-no-screen",
                    "-arm-supported-modes", "thumb"
            };
        } else if (arch==ArchType.X86_64) {
            return new String[]{
                    "binsec",
                    "-sse",
                    "-sse-engine", "concrete",
                    "-sse-script", temp.getPath(),
                    binpath,
                    "-emul-perm", String.format("%s/resources/permissions%02d.config", System.getenv("PRECA_PATH"), arch_size),
                    "-sse-timeout", emultimeout.toString(),
                    "-sse-no-screen"
            };
        }
        assert false : "Architecture not supported";
        return null;

        }

    String calling_convention(int index){
        assert(index>=0);
        if(arch==ArchType.X86_64){
            if(index<calling_convention_x86_64.length){
                return calling_convention_x86_64[index];
            }
            else {
                last_offset=last_offset+8;
                return "@[rsp+"+last_offset+",8]";
            }
        }
        else if(arch==ArchType.ARM32){
            if(index<calling_convention_arm32.length){
                return calling_convention_arm32[index];
            }
            else {
                last_offset=last_offset+4;
                return "@[sp+"+last_offset+",4]";
            }
        }

        assert false : "Architecture not supported";
        return "";
    }

    int id = 1;

    Runtime runtime;

    void set_arch_size(ArchType arch){
        switch(arch){
            case ARM32: this.arch_size=32; break;
            case X86_64: this.arch_size=64; break;
            default: assert false;
        }

    }

    public Binsec(String conffile, CellType[] types, boolean[] globals, ArchType arch) {

        this.types = types;
        this.globals = globals;
        this.runtime = Runtime.getRuntime();
        this.arch=arch;
        set_arch_size(arch);
        reset_last_offset();




        try (BufferedReader br = Files.newBufferedReader(Paths.get(conffile))) {
            // read line by line
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("bin:")) {
                    binpath = line.split(":")[1].strip();
                } else if (line.startsWith("emultimeout:")) {
                    emultimeout = Integer.parseInt(line.split(":")[1].strip());
                } else if (line.startsWith("addr_globals:")) {
                    String[] saddrs = line.split(":")[1].strip().split(",");
                    addrs_global = new Integer[saddrs.length];
                    for (int i = 0; i < saddrs.length; i++) {
                        addrs_global[i] = Integer.decode(saddrs[i]);
                    }
                } else if (line.startsWith("binsec:")) {
                    Path filePath = Path.of(line.split(":")[1].strip());
                    binsec_init = Files.readString(filePath);
                }
            }

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
    }


    private HashMap<Integer, Integer> getGlobalMapping(Integer[] inputs) {
        int input_index = 0;
        int cell_index = 0;
        int global_index = 0;
        HashMap<Integer, Integer> global_mapping = new HashMap<>();

        while (input_index < inputs.length) {
            if (globals[cell_index]) {
                global_mapping.put(inputs[input_index], addrs_global[global_index]);
                global_index++;
                input_index++;
            }
            switch (types[cell_index]) {
                case PTR:
                    input_index++; // the value
                    input_index++; // the size
                    break;
                case INT:
                case UINT:
                case UINT8:
                case UINT16:
                case BOOL:
                    input_index++;
                    break;
                default:
                    assert false;
            }

            cell_index++;
        }
        return global_mapping;
    }

    private String setinputs(Integer[] inputs) {
        int input_index = 0;
        int cell_index = 0;
        int global_index = 0;
        HashMap<Integer, Integer> global_mapping = getGlobalMapping(inputs);

        String config = "";
        while (input_index < inputs.length) {

            Integer ref = null;
            Integer value = null;
            Integer size = null;
            String dba_cmd = "";

            if (globals[cell_index]) {
                ref = addrs_global[global_index];
                global_index++;
                input_index++;
            }

            switch (types[cell_index]) {
                case PTR:
                    value = global_mapping.containsKey(inputs[input_index]) ?
                            global_mapping.get(inputs[input_index]) : inputs[input_index] * 1000;
                    input_index++;
                    size = inputs[input_index];
                    input_index++;

                    if (ref == null) dba_cmd += String.format("%s := 0x%016x\n", calling_convention(cell_index), value);
                    else dba_cmd += String.format("@[0x%016x, %d] := 0x%016x\n", ref, arch_size, value);

                    if (value != 0) {
                        for (int offset = 0; offset < size-1; offset++) {
                            dba_cmd += String.format("@[0x%016x+%d,1] := 0x%02x\n", value, offset, 41 & 0xff); // TODO random byte non null
                        }
                        dba_cmd += String.format("@[0x%016x+%d, 1] := 0x00\n\n", value, size-1);
                    }
                    break;
                case INT:
                case UINT:
                case UINT8:
                case UINT16:
                case BOOL:
                    value = inputs[input_index];
                    input_index++;
                    if (ref == null) dba_cmd += String.format("%s := %d\n", calling_convention(cell_index), value);
                    else dba_cmd += String.format("@[%d, %d] := %d\n", ref, arch_size, value);
                    break;
                default:
                    assert false;
            }

            config += dba_cmd;
            cell_index++;
        }
        reset_last_offset();
        //System.out.println(config);
        return config;

    }

    public int call(Integer[] inputs, String query) {
        String config = binsec_init + "\n\n" + setinputs(inputs);


        FileWriter myWriter;
        File temp = null;
        try {
            temp = File.createTempFile("binsec_config_", ".txt");
            myWriter = new FileWriter(temp);
            myWriter.write(config);
            myWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assert temp != null : "temp file should not be null";

        Process proc;
        int res = 0;
        try {

            //Start time
            long begin = System.currentTimeMillis();

            proc = runtime.exec(binsec_command(temp));
            proc.waitFor();
            if (proc.exitValue() != 0) {
                Files.deleteIfExists(temp.toPath());
                assert false : "Error while running Binsec";
            }


            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            Boolean stderr_empty = true;
            String s = null;
            while ((s = stdErr.readLine()) != null) {
                stderr_empty = false;
            }
            assert stderr_empty : "Error in Binsec output";

            //Pattern pattern = Pattern.compile("Formula(?:\\s*@ 0x[0-9A-Fa-f]{8})?(.*?)(\\[sse:info\\]|$)", Pattern.DOTALL);
            //Pattern pattern = Pattern.compile("(?s)(?!.*anonymous)(?<=\\[sse:debug\\]\\s*0x[0-9A-Fa-f]+\\s*)[^\\[]*(?=(\\[sse:result\\]|$))");
            StringBuilder outputBuffer = new StringBuilder();

            while ((s = stdInput.readLine()) != null) {
                //save proc result
            	assert !s.contains("Cut path") : "Cut path !!!!!!!!!!!!!!!!!";
                outputBuffer.append(s).append("\n");
            }
            String output = outputBuffer.toString();
            if (output.contains("[EMUL ERROR]")) {
                res = 1;
            }

            if (System.currentTimeMillis() - begin > emultimeout * 1000) res = 1;


            Files.deleteIfExists(temp.toPath());
        } catch (IOException | InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        return res;
    }
}
