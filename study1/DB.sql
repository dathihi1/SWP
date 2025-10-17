create table `templates/products`
(
    id          bigint       not null
        primary key,
    description varchar(255) null,
    name        varchar(255) null,
    price       double       null
);

create table `templates/products_seq`
(
    next_val bigint null
);

create table user
(
    id         bigint auto_increment
        primary key,
    username   varchar(255)                                               not null,
    password   varchar(255)                                               not null,
    email      varchar(255)                                               not null,
    phone      varchar(20)                                                null,
    full_name  varchar(100)                                               null,
    role       enum ('USER', 'ADMIN', 'SELLER') default 'USER'            null,
    status     enum ('ACTIVE', 'LOCKED')        default 'ACTIVE'          null,
    isDelete   tinyint(1)                       default 0                 null,
    createdBy  varchar(50)                                                null,
    createdAt  timestamp                        default CURRENT_TIMESTAMP null,
    updatedAt  timestamp                                                  null,
    deletedBy  varchar(50)                                                null,
    created_by varchar(50)                                                null,
    deleted_by varchar(50)                                                null,
    is_delete  bit                                                        not null,
    updated_at datetime(6)                                                null,
    created_at timestamp                        default CURRENT_TIMESTAMP null,
    constraint email
        unique (email),
    constraint username
        unique (username)
);

create table auditlog
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                               null,
    action     varchar(255)                         not null,
    details    text                                 null,
    ip_address varchar(50)                          null,
    isDelete   tinyint(1) default 0                 null,
    createdBy  varchar(50)                          null,
    createdAt  timestamp  default CURRENT_TIMESTAMP null,
    updatedAt  timestamp                            null,
    deletedBy  varchar(50)                          null,
    created_at datetime(6)                          not null,
    created_by varchar(50)                          null,
    deleted_by varchar(50)                          null,
    is_delete  bit                                  not null,
    updated_at datetime(6)                          null,
    constraint FKaj0ht2xgc2rpkerifiwicy28s
        foreign key (user_id) references user (id)
);

create table bankaccount
(
    id             bigint auto_increment
        primary key,
    user_id        bigint                               not null,
    verified_at    timestamp                            null,
    isDelete       tinyint(1) default 0                 null,
    createdBy      varchar(50)                          null,
    createdAt      timestamp  default CURRENT_TIMESTAMP null,
    updatedAt      timestamp                            null,
    deletedBy      varchar(50)                          null,
    created_at     datetime(6)                          not null,
    created_by     varchar(50)                          null,
    deleted_by     varchar(50)                          null,
    is_delete      bit                                  not null,
    updated_at     datetime(6)                          null,
    account_number varchar(50)                          not null,
    bank_account   varchar(200)                         null,
    bank_name      varchar(100)                         not null,
    constraint fk_bank_user
        foreign key (user_id) references user (id)
);

create table paymentlog
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                                                          not null,
    type       enum ('DEPOSIT', 'WITHDRAW')                                    not null,
    amount     decimal(15, 2)                                                  not null,
    gateway    varchar(50)                           default 'VNPAY'           null,
    status     enum ('PENDING', 'SUCCESS', 'FAILED') default 'PENDING'         null,
    isDelete   tinyint(1)                            default 0                 null,
    createdBy  varchar(50)                                                     null,
    createdAt  timestamp                             default CURRENT_TIMESTAMP null,
    updatedAt  timestamp                                                       null,
    deletedBy  varchar(50)                                                     null,
    created_at datetime(6)                                                     not null,
    created_by varchar(50)                                                     null,
    deleted_by varchar(50)                                                     null,
    is_delete  bit                                                             not null,
    updated_at datetime(6)                                                     null,
    constraint fk_payment_user
        foreign key (user_id) references user (id)
);

create table shop
(
    id              bigint auto_increment
        primary key,
    user_id         bigint                               not null,
    shop_name       varchar(100)                         not null,
    cccd            varchar(255)                         null,
    bank_account_id bigint                               not null,
    isDelete        tinyint(1) default 0                 null,
    createdBy       varchar(50)                          null,
    createdAt       timestamp  default CURRENT_TIMESTAMP null,
    updatedAt       timestamp                            null,
    deletedBy       varchar(50)                          null,
    created_at      datetime(6)                          not null,
    created_by      varchar(50)                          null,
    deleted_by      varchar(50)                          null,
    is_delete       bit                                  not null,
    updated_at      datetime(6)                          null,
    constraint user_id
        unique (user_id),
    constraint fk_shop_bank
        foreign key (bank_account_id) references bankaccount (id),
    constraint fk_shop_user
        foreign key (user_id) references user (id)
);

create table product
(
    id          bigint auto_increment
        primary key,
    shop_id     bigint                                                      not null,
    type        varchar(50)                                                 not null,
    name        varchar(100)                                                not null,
    description text                                                        null,
    price       decimal(15, 2)                                              not null,
    quantity    int                               default 1                 null,
    unique_key  varchar(255)                                                not null,
    status      enum ('ACTIVE', 'SOLD', 'BANNED') default 'ACTIVE'          null,
    isDelete    tinyint(1)                        default 0                 null,
    createdBy   varchar(50)                                                 null,
    createdAt   timestamp                         default CURRENT_TIMESTAMP null,
    updatedAt   timestamp                                                   null,
    deletedBy   varchar(50)                                                 null,
    created_at  datetime(6)                                                 not null,
    created_by  varchar(50)                                                 null,
    deleted_by  varchar(50)                                                 null,
    is_delete   bit                                                         not null,
    updated_at  datetime(6)                                                 null,
    constraint uq_unique_product
        unique (unique_key),
    constraint fk_product_shop
        foreign key (shop_id) references shop (id)
);

create table cart
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                               not null,
    product_id bigint                               not null,
    quantity   int        default 1                 null,
    isDelete   tinyint(1) default 0                 null,
    createdBy  varchar(50)                          null,
    createdAt  timestamp  default CURRENT_TIMESTAMP null,
    updatedAt  timestamp                            null,
    deletedBy  varchar(50)                          null,
    created_at datetime(6)                          not null,
    created_by varchar(50)                          null,
    deleted_by varchar(50)                          null,
    is_delete  bit                                  not null,
    updated_at datetime(6)                          null,
    constraint fk_cart_product
        foreign key (product_id) references product (id),
    constraint fk_cart_user
        foreign key (user_id) references user (id)
);

create table cart_item
(
    id         bigint auto_increment
        primary key,
    quantity   int    not null,
    cart_id    bigint not null,
    product_id bigint not null,
    constraint FK1uobyhgl1wvgt1jpccia8xxs3
        foreign key (cart_id) references cart (id),
    constraint FKjcyd5wv4igqnw413rgxbfu4nv
        foreign key (product_id) references product (id)
);

create table review
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                               not null,
    product_id bigint                               not null,
    rating     int                                  null,
    comment    text                                 null,
    isDelete   tinyint(1) default 0                 null,
    createdBy  varchar(50)                          null,
    createdAt  timestamp  default CURRENT_TIMESTAMP null,
    updatedAt  timestamp                            null,
    deletedBy  varchar(50)                          null,
    created_at datetime(6)                          not null,
    created_by varchar(50)                          null,
    deleted_by varchar(50)                          null,
    is_delete  bit                                  not null,
    updated_at datetime(6)                          null,
    constraint fk_review_product
        foreign key (product_id) references product (id),
    constraint fk_review_user
        foreign key (user_id) references user (id),
    check (`rating` between 1 and 5)
);

create table transaction
(
    id         bigint auto_increment
        primary key,
    buyer_id   bigint                                                                   not null,
    product_id bigint                                                                   not null,
    amount     decimal(15, 2)                                                           not null,
    status     enum ('PENDING', 'HOLD', 'RELEASED', 'REFUND') default 'PENDING'         null,
    isDelete   tinyint(1)                                     default 0                 null,
    createdBy  varchar(50)                                                              null,
    createdAt  timestamp                                      default CURRENT_TIMESTAMP null,
    updatedAt  timestamp                                                                null,
    deletedBy  varchar(50)                                                              null,
    created_at datetime(6)                                                              not null,
    created_by varchar(50)                                                              null,
    deleted_by varchar(50)                                                              null,
    is_delete  bit                                                                      not null,
    updated_at datetime(6)                                                              null,
    constraint fk_trans_buyer
        foreign key (buyer_id) references user (id),
    constraint fk_trans_product
        foreign key (product_id) references product (id)
);

create table complaint
(
    id             bigint auto_increment
        primary key,
    transaction_id bigint                                                             not null,
    buyer_id       bigint                                                             not null,
    description    text                                                               null,
    status         enum ('PENDING', 'APPROVED', 'REJECTED') default 'PENDING'         null,
    isDelete       tinyint(1)                               default 0                 null,
    createdBy      varchar(50)                                                        null,
    createdAt      timestamp                                default CURRENT_TIMESTAMP null,
    updatedAt      timestamp                                                          null,
    deletedBy      varchar(50)                                                        null,
    created_at     datetime(6)                                                        not null,
    created_by     varchar(50)                                                        null,
    deleted_by     varchar(50)                                                        null,
    is_delete      bit                                                                not null,
    updated_at     datetime(6)                                                        null,
    constraint fk_complaint_buyer
        foreign key (buyer_id) references user (id),
    constraint fk_complaint_trans
        foreign key (transaction_id) references transaction (id)
);

create table wallet
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                                   not null,
    balance    decimal(15, 2) default 0.00              null,
    isDelete   tinyint(1)     default 0                 null,
    createdBy  varchar(50)                              null,
    createdAt  timestamp      default CURRENT_TIMESTAMP null,
    updatedAt  timestamp                                null,
    deletedBy  varchar(50)                              null,
    created_by varchar(50)                              null,
    deleted_by varchar(50)                              null,
    is_delete  bit                                      not null,
    updated_at datetime(6)                              null,
    created_at timestamp      default CURRENT_TIMESTAMP null,
    constraint user_id
        unique (user_id),
    constraint fk_wallet_user
        foreign key (user_id) references user (id)
);

create table wallethistory
(
    id           bigint auto_increment
        primary key,
    wallet_id    bigint                                             not null,
    type         enum ('DEPOSIT', 'WITHDRAW', 'PURCHASE', 'REFUND') not null,
    amount       decimal(15, 2)                                     not null,
    reference_id bigint                                             null,
    description  longtext                                           null,
    isDelete     tinyint(1) default 0                               null,
    createdBy    varchar(50)                                        null,
    createdAt    timestamp  default CURRENT_TIMESTAMP               null,
    updatedAt    timestamp                                          null,
    deletedBy    varchar(50)                                        null,
    created_at   datetime(6)                                        null,
    created_by   varchar(50)                                        null,
    deleted_by   varchar(50)                                        null,
    is_delete    bit                                                null,
    status       enum ('CANCELED', 'FAILED', 'PENDING', 'SUCCESS')  null,
    updated_at   datetime(6)                                        null,
    constraint fk_wallet_history
        foreign key (wallet_id) references wallet (id)
);

create table withdrawrequest
(
    id         bigint auto_increment
        primary key,
    shop_id    bigint                                                             not null,
    amount     decimal(15, 2)                                                     not null,
    status     enum ('PENDING', 'APPROVED', 'REJECTED') default 'PENDING'         null,
    isDelete   tinyint(1)                               default 0                 null,
    createdBy  varchar(50)                                                        null,
    createdAt  timestamp                                default CURRENT_TIMESTAMP null,
    updatedAt  timestamp                                                          null,
    deletedBy  varchar(50)                                                        null,
    created_at datetime(6)                                                        not null,
    created_by varchar(50)                                                        null,
    deleted_by varchar(50)                                                        null,
    is_delete  bit                                                                not null,
    updated_at datetime(6)                                                        null,
    constraint fk_withdraw_shop
        foreign key (shop_id) references shop (id)
);

