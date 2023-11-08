/****************************************************************************/
/*  This file is part of PRECA.                                             */
/*  PRECA is part of the BINSEC toolbox for binary-level program analysis.  */
/*                                                                          */
/*  Copyright (C) 2019-2023                                                 */
/*    CEA (Commissariat à l'énergie atomique et aux énergies                */
/*         alternatives)                                                    */
/*                                                                          */
/*  you can redistribute it and/or modify it under the terms of the GNU     */
/*  Lesser General Public License as published by the Free Software         */
/*  Foundation, version 2.1.                                                */
/*                                                                          */
/*  It is distributed in the hope that it will be useful,                   */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of          */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           */
/*  GNU Lesser General Public License for more details.                     */
/*                                                                          */
/*  See the GNU Lesser General Public License version 2.1                   */
/*  for more details (enclosed in the file licenses/LGPLv2.1).              */
/*                                                                          */
/****************************************************************************/
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

import io.github.binsec.core.acqvariable.CellType;


public class Binsec {

	String binpath;
	String binsec_init;
	Integer emultimeout = 60;
	CellType[] types;
	boolean[] globals;
	Integer[] addrs_global;
	String[] calling_convention = new String[] {
			"rdi",
			"rsi",
			"rdx",
			"rcx",
			"r8",
			"r9",
			"@[rsp+8, 8]",
			"@[rsp+16, 8]",
			"@[rsp+24, 8]"
	};
	
	Runtime runtime;
	
	public Binsec(String conffile, CellType[] types, boolean[] globals) {
		
		this.types = types;
		this.globals = globals;
		this.runtime = Runtime.getRuntime();
		
		try (BufferedReader br = Files.newBufferedReader(Paths.get(conffile))) {
            // read line by line
            String line;
            while ((line = br.readLine()) != null) {
            	if (line.startsWith("bin:")) {
            		binpath = line.split(":")[1].strip();
            	}
            	else if (line.startsWith("emultimeout:")) {
            		emultimeout = Integer.parseInt(line.split(":")[1].strip());
            	}
            	else if (line.startsWith("addr_globals:")) {
            		String[] saddrs = line.split(":")[1].strip().split(","); 
            		addrs_global = new Integer[saddrs.length];
            		for (int i = 0; i < saddrs.length; i++) {
            			addrs_global[i] = Integer.decode(saddrs[i]);
            		}
            	}
            	else if (line.startsWith("binsec:")) {
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
				
				if (ref == null) dba_cmd += String.format("%s := 0x%016x\n", calling_convention[cell_index], value);
				else dba_cmd += String.format("@[0x%016x, %d] := 0x%016x\n", ref, 64, value);
				
				if (value != 0) {
					for (int offset = 0; offset < size-1; offset++) {
						dba_cmd += String.format("@[0x%016x+%d,1] := 0x%02x\n", value, offset, 41 & 0xff); // TODO random byte non null
					}
					dba_cmd += String.format("@[0x%016x+%d, 1] := 0x00\n\n", value, size-1);
				}
				break;
			case INT:
			case UINT:
				value = inputs[input_index];
				input_index++;
				if (ref == null) dba_cmd += String.format("%s := %d\n", calling_convention[cell_index], value);
				else dba_cmd += String.format("@[%d, %d] := %d\n", ref, 64, value);
				break;
			default:
				assert false;
			}
			
			config += dba_cmd;
			cell_index++;
		}
		return config;
	}
	
	public int call(Integer[] inputs) {
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
		assert temp != null: "temp file should not be null";
		
		Process proc;
		int res = 0;
		
		try {
			
			//Start time
		    long begin = System.currentTimeMillis();
			
			proc = runtime.exec(new String[] { 
					"binsec",
					"-sse", 
					"-sse-engine", "concrete", 
					"-sse-script", temp.getPath(),
					binpath, 
					"-emul-perm", String.format("%s/resources/permissions.config", System.getenv("PRECA_PATH")),
					"-sse-timeout", emultimeout.toString(),
					"-sse-no-screen"
			});
			proc.waitFor();

                        if (proc.exitValue() != 0) {
			    Files.deleteIfExists(temp.toPath());
			    assert false: "Error while running Binsec";
                        }

			
			BufferedReader stdInput = new BufferedReader(new 
				     InputStreamReader(proc.getInputStream()));
			
			String s = null;
			while ((s = stdInput.readLine()) != null) {
			    if (s.contains("[EMUL ERROR]")) {
			    	res = 1;
			    }
			}
			
		    if (System.currentTimeMillis() - begin > emultimeout*1000) res = 1;
			
			Files.deleteIfExists(temp.toPath());
		} catch (IOException | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return res;
	}
}
