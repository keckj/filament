/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package main

import (
	"bufio"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"text/template"
)

func createJavaCodeGenerator() func(*os.File, string, Scope) {
	customExtensions := template.FuncMap{
		"javadoc": func(desc string, depth int) string {
			return "// " + desc
		},
		"java_type": func(cpptype string) string {
			return cpptype
		},
		"java_value": func(cppval string) string {
			return cppval
		},
	}

	templ := template.New("beamsplitter").Funcs(customExtensions)
	templ = template.Must(templ.ParseFiles("java.template"))
	return func(file *os.File, section string, definition Scope) {
		err := templ.ExecuteTemplate(file, "CppStructReader", definition)
		if err != nil {
			log.Fatal(err.Error())
		}
	}
}

func EditJava(definitions []Scope, classname string, folder string) {
	path := filepath.Join(folder, classname+".java")
	var codelines []string
	{
		sourceFile, err := os.Open(path)
		if err != nil {
			log.Fatal(err)
		}
		defer sourceFile.Close()
		lineScanner := bufio.NewScanner(sourceFile)
		foundMarker := false
		for lineNumber := 1; lineScanner.Scan(); lineNumber++ {
			codeline := lineScanner.Text()
			if strings.Contains(codeline, CodelineMarker) {
				foundMarker = true
				break
			}
			codelines = append(codelines, codeline)
		}
		if !foundMarker {
			log.Fatal("Unable to find marker line in Java file.")
		}
	}
	file, err := os.Create(path)
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()
	defer fmt.Println("Edited " + path)
	for _, codeline := range codelines {
		file.WriteString(codeline)
		file.WriteString("\n")
	}
	file.WriteString("    // " + CodelineMarker + "\n")

	generate := createJavaCodeGenerator()
	for _, definition := range definitions {
		switch definition.(type) {
		case *StructDefinition:
			generate(file, "Struct", definition)
		case *EnumDefinition:
			generate(file, "Enum", definition)
		}
	}

	file.WriteString("}\n")
}
