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

// Returns a templating function that automatically checks for fatal errors. The returned function
// takes an output stream, a template name to invoke, and a template context object.
func createJsCodeGenerator(namespace string) func(*os.File, string, TypeDefinition) {
	jsPrefix := ""
	classPrefix := ""
	cppPrefix := ""
	if namespace != "" {
		jsPrefix = namespace + "$"
		classPrefix = namespace + ".prototype."
		cppPrefix = namespace + "::"
	}
	// These template extensions are used to transmogrify C++ symbols and value literals into
	// JavaScript. We mostly don't need to do anything since the parser has already done some
	// massaging and verification (e.g. it removed the trailing "f" from floating point literals).
	// However enums need some special care here. Emscripten bindings are flat, so our own
	// convention is to use $ for the scoping delimiter, which is a legal symbol character in JS.
	// However we still use . to separate the enum value from the enum type, because emscripten has
	// first-class support for class enums.
	customExtensions := template.FuncMap{
		"qualifiedtype": func(typename string) string {
			typename = strings.ReplaceAll(typename, "::", "$")
			return typename
		},
		"qualifiedvalue": func(name string) string {
			count := strings.Count(name, "::")
			if count > 0 {
				name = "Filament." + jsPrefix + name
			}
			name = strings.Replace(name, "::", "$", count-1)
			name = strings.Replace(name, "::", ".", 1)
			return name
		},
		"tstype": func(cpptype string) string {
			if strings.HasPrefix(cpptype, "math::") {
				return cpptype[6:]
			}
			switch cpptype {
			case "float", "uint8_t", "uint32_t", "uint16_t":
				return "number"
			case "bool":
				return "boolean"
			case "LinearColorA":
				return "float4"
			case "LinearColor":
				return "float3"
			}
			return jsPrefix + strings.ReplaceAll(cpptype, "::", "$")
		},
		"jsprefix":    func() string { return jsPrefix },
		"cprefix":     func() string { return cppPrefix },
		"classprefix": func() string { return classPrefix },
	}

	templ := template.New("beamsplitter").Funcs(customExtensions)
	templ = template.Must(templ.ParseFiles("javascript.template"))
	return func(file *os.File, section string, definition TypeDefinition) {
		err := templ.ExecuteTemplate(file, section, definition)
		if err != nil {
			log.Fatal(err.Error())
		}
	}
}

func EmitJavaScript(definitions []TypeDefinition, namespace string, outputFolder string) {
	generate := createJsCodeGenerator(namespace)
	{
		path := filepath.Join(outputFolder, "jsbindings_generated.cpp")
		file, err := os.Create(path)
		if err != nil {
			log.Fatal(err)
		}
		defer file.Close()
		defer fmt.Println("Generated " + path)

		generate(file, "JsBindingsHeader", nil)

		for _, definition := range definitions {
			switch definition.(type) {
			case *StructDefinition:
				generate(file, "JsBindingsStruct", definition)
			}
		}
		generate(file, "JsBindingsFooter", nil)
	}
	{
		path := filepath.Join(outputFolder, "jsenums_generated.cpp")
		file, err := os.Create(path)
		if err != nil {
			log.Fatal(err)
		}
		defer file.Close()
		defer fmt.Println("Generated " + path)

		generate(file, "JsEnumsHeader", nil)

		for _, definition := range definitions {
			switch definition.(type) {
			case *EnumDefinition:
				generate(file, "JsEnum", definition)
			}
		}

		generate(file, "JsEnumsFooter", nil)
	}
	{
		path := filepath.Join(outputFolder, "extensions_generated.js")
		file, err := os.Create(path)
		if err != nil {
			log.Fatal(err)
		}
		defer file.Close()
		defer fmt.Println("Generated " + path)

		generate(file, "JsExtensionsHeader", nil)

		for _, definition := range definitions {
			switch definition.(type) {
			case *StructDefinition:
				generate(file, "JsExtension", definition)
			}
		}
		generate(file, "JsExtensionsFooter", nil)
	}
}

func EditTypeScript(definitions []TypeDefinition, namespace string, folder string) {
	path := filepath.Join(folder, "filament.d.ts")
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
			log.Fatal("Unable to find marker line in TypeScript file.")
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
	file.WriteString("// " + CodelineMarker + "\n")

	generate := createJsCodeGenerator(namespace)
	for _, definition := range definitions {
		switch definition.(type) {
		case *StructDefinition:
			generate(file, "TsStruct", definition)
		case *EnumDefinition:
			generate(file, "TsEnum", definition)
		}
	}
}
