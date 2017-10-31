/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liferay.blade.api;

import java.util.List;

/**
 * @author Gregory Amerson
 */
public interface JavaFile extends SourceFile {

	List<SearchResult> findCatchExceptions(String[] exceptions);

	List<SearchResult> findImplementsInterface(String interfaceName);

	SearchResult findImport(String importName);

	List<SearchResult> findImports(String[] imports);

	List<SearchResult> findMethodDeclaration(String name, String[] params, String returnType);

	List<SearchResult> findMethodInvocations(String typeHint, String expressionValue, String methodName, String[] methodParamTypes);

	SearchResult findPackage(String packageName);

	List<SearchResult> findServiceAPIs(String[] serviceApiPrefixes);

	List<SearchResult> findSuperClass(String superClassName);

	List<SearchResult> findQualifiedName(String QualifiedName);
}
