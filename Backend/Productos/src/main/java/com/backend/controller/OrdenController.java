   package com.backend.controller;

   import com.backend.dto.OrdenRequest;
   import com.backend.dto.OrdenResponse;
   import com.backend.service.OrdenService;
   import jakarta.validation.Valid;
   import org.springframework.http.HttpStatus;
   import org.springframework.http.ResponseEntity;
   import org.springframework.security.core.annotation.AuthenticationPrincipal;
   import org.springframework.security.oauth2.jwt.Jwt;
   import org.springframework.web.bind.annotation.*;

   @RestController
   @RequestMapping("/api/ordenes")
   public class OrdenController {

       private final OrdenService ordenService;

       public OrdenController(OrdenService ordenService) {
           this.ordenService = ordenService;
       }

       @PostMapping
       public ResponseEntity<OrdenResponse> crear(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody OrdenRequest request) {
           OrdenResponse respuesta = ordenService.crear(jwt.getClaimAsString("oid"), request);
           return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
       }
   }