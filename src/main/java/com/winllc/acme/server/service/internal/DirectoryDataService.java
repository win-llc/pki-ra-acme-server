package com.winllc.acme.server.service.internal;

import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.DirectoryData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/directory")
public class DirectoryDataService {
    //TODO CRUD service for Directory Data

    @GetMapping
    public ResponseEntity<?> getAll(){
        //TODO
        return null;
    }

    @GetMapping("/byName/{name}")
    public ResponseEntity<?> getByName(@PathVariable String name){
        //TODO
        return null;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDirectory(DirectoryData directoryData){
        //TODO
        return null;
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateDirectory(DirectoryData directoryData){
        //TODO
        return null;
    }

    @GetMapping("/delete/{name}")
    public ResponseEntity<?> deleteDirectory(@PathVariable String name){
        //TODO
        return null;
    }
}
