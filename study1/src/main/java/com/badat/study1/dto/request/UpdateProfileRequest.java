package com.badat.study1.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateProfileRequest {
    
    @Size(min = 2, max = 50, message = "Họ và tên phải có từ 2-50 ký tự")
    @Pattern(regexp = "^[a-zA-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼẾỀỂỄỆỈĨỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴÝỶỸĐăạảấầẩẫậắằẳẵặẹẻẽếềểễệỉĩịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹđ\\s]{2,50}$", 
             message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng")
    String fullName;
    
    @Pattern(regexp = "^(0|\\+84)[35789][0-9]{8}$", 
             message = "Số điện thoại phải bắt đầu bằng 0 hoặc +84, theo sau là 3,5,7,8,9 và 8 chữ số")
    String phone;
}
