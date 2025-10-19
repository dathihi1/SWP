package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallethistory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletHistory {

	public enum Type {
		DEPOSIT, WITHDRAW, PURCHASE, REFUND, SALE, COMMISSION
	}

	public enum Status {
		PENDING, SUCCESS, FAILED, CANCELED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = "wallet_id", nullable = false)
	Long walletId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	Type type;

	@Column(nullable = false, precision = 15, scale = 2)
	BigDecimal amount;

	@Column(name = "reference_id")
	String referenceId;

	@Lob
	String description;

	@Column(name = "is_delete")
	Boolean isDelete;

	@Column(name = "created_by")
	String createdBy;

	@Column(name = "created_at")
	Instant createdAt;

	@Column(name = "updated_at")
	Instant updatedAt;

	@Column(name = "deleted_by")
	String deletedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	Status status;
}


