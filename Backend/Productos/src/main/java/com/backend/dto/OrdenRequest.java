   package com.backend.dto;

   import jakarta.validation.Valid;
   import jakarta.validation.constraints.Min;
   import jakarta.validation.constraints.NotEmpty;
   import jakarta.validation.constraints.NotNull;
   import lombok.AllArgsConstructor;
   import lombok.Data;
   import lombok.NoArgsConstructor;

   import java.util.List;

   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public class OrdenRequest {

       @NotEmpty(message = "La orden debe tener al menos un producto")
       @Valid
       private List<Item> items;

       @Data
       @NoArgsConstructor
       @AllArgsConstructor
       public static class Item {

           @NotNull(message = "El producto es obligatorio")
           private Long productoId;

           @NotNull(message = "La cantidad es obligatoria")
           @Min(value = 1, message = "La cantidad debe ser al menos 1")
           private Integer cantidad;
       }
   }