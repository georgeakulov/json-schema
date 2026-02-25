
* **1.0.0** - Draft 2020-12 spec implementation
    

* **1.1.0** - Added Draft  2019-09 spec implementation
    <details>
        <summary>more</summary>
  
        - Remove guava library from dependencies
    </details>


* **1.2.0** - Added Draft 7 spec implementation
    
    <details>
        <summary>more</summary>
  
       - Add support for Content vocabulary (Draft7+)
       - Add support for add or redefine custom format validators
       - Add support for add or redefine custom contentEncoding validator
       - Add support for add or redefine custom contentMediaType validator
    </details>


* **1.2.1** - Changed package names to comply with Maven requirements


* **1.2.2** - Compress resources and add publishing config

* **1.2.3** - Minimize eternal schemas loading. 

    <details>
        <summary>more</summary>
  
        - the content of non standard $schema uri, can be predefined by IExternalResolver
        - remove unnecessary console output 
        - the content of specification schema and its vocabularies included in library and may be used 
        - the internal loaders may be disabled
    </details>
