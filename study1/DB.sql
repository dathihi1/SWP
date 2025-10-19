create table bankaccount
(
    id             bigint auto_increment
        primary key,
    account_number varchar(50)  not null,
    bank_account   varchar(200) null,
    bank_name      varchar(100) not null,
    created_at     datetime(6)  null,
    is_delete      bit          not null,
    user_id        bigint       not null
);

create table shop
(
    id              bigint auto_increment
        primary key,
    bank_account_id bigint       not null,
    cccd            varchar(255) null,
    created_at      datetime(6)  not null,
    is_delete       bit          not null,
    shop_name       varchar(100) not null,
    user_id         bigint       not null,
    constraint UKlaiiqc8bblm23bejvksacwogf
        unique (user_id)
);

create table stall
(
    id                   bigint auto_increment
        primary key,
    business_type        varchar(50)  null,
    created_at           datetime(6)  not null,
    detailed_description text         null,
    discount_percentage  double       null,
    is_delete            bit          not null,
    shop_id              bigint       not null,
    short_description    varchar(500) null,
    stall_category       varchar(50)  null,
    stall_image_data     longblob     null,
    stall_name           varchar(100) not null,
    status               varchar(20)  not null
);

create table product
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)                       not null,
    created_by  varchar(50)                       null,
    deleted_by  varchar(50)                       null,
    is_delete   bit                               not null,
    updated_at  datetime(6)                       null,
    description text                              null,
    name        varchar(100)                      not null,
    price       decimal(15, 2)                    not null,
    quantity    int                               null,
    shop_id     bigint                            not null,
    stall_id    bigint                            not null,
    status      enum ('AVAILABLE', 'UNAVAILABLE') null,
    type        varchar(50)                       not null,
    unique_key  varchar(255)                      not null,
    constraint UKjy1utko6jkwj9qejn23p5u8pj
        unique (unique_key),
    constraint FK94hgg8hlqfqfnt3dag950vm7n
        foreign key (shop_id) references shop (id),
    constraint FKi1hicbqi0prc86l1bklij9mu2
        foreign key (stall_id) references stall (id)
);

create table user
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)                      not null,
    created_by  varchar(50)                      null,
    deleted_by  varchar(50)                      null,
    is_delete   bit                              not null,
    updated_at  datetime(6)                      null,
    email       varchar(100)                     not null,
    full_name   varchar(100)                     null,
    password    varchar(255)                     not null,
    phone       varchar(20)                      null,
    provider    varchar(20)                      null,
    provider_id varchar(100)                     null,
    role        enum ('ADMIN', 'SELLER', 'USER') not null,
    status      enum ('ACTIVE', 'LOCKED')        not null,
    username    varchar(50)                      not null,
    constraint UKob8kqyqqgmefl0aco34akdtpe
        unique (email),
    constraint UKsb8bbouer5wak8vyiiy4pf2bx
        unique (username)
);

create table auditlog
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6)  not null,
    created_by varchar(50)  null,
    deleted_by varchar(50)  null,
    is_delete  bit          not null,
    updated_at datetime(6)  null,
    action     varchar(255) not null,
    details    text         null,
    ip_address varchar(50)  null,
    user_id    bigint       null,
    constraint FKaj0ht2xgc2rpkerifiwicy28s
        foreign key (user_id) references user (id)
);

create table cart
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6) not null,
    created_by varchar(50) null,
    deleted_by varchar(50) null,
    is_delete  bit         not null,
    updated_at datetime(6) null,
    user_id    bigint      not null,
    constraint UK9emlp6m95v5er2bcqkjsw48he
        unique (user_id),
    constraint FKl70asp4l4w0jmbm1tqyofho4o
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

create table paymentlog
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6)                           not null,
    created_by varchar(50)                           null,
    deleted_by varchar(50)                           null,
    is_delete  bit                                   not null,
    updated_at datetime(6)                           null,
    amount     decimal(15, 2)                        not null,
    gateway    varchar(50)                           null,
    status     enum ('FAILED', 'PENDING', 'SUCCESS') null,
    type       enum ('DEPOSIT', 'WITHDRAW')          not null,
    user_id    bigint                                not null,
    constraint FKcbn62a6eucdjstsody0822by6
        foreign key (user_id) references user (id)
);

create table review
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6) not null,
    created_by varchar(50) null,
    deleted_by varchar(50) null,
    is_delete  bit         not null,
    updated_at datetime(6) null,
    comment    text        null,
    product_id bigint      not null,
    rating     int         null,
    user_id    bigint      not null,
    constraint FKiyf57dy48lyiftdrf7y87rnxi
        foreign key (user_id) references user (id),
    constraint FKiyof1sindb9qiqr9o8npj8klt
        foreign key (product_id) references product (id)
);

create table upload_history
(
    id             bigint auto_increment
        primary key,
    created_at     datetime(6)  not null,
    failure_count  int          null,
    file_name      varchar(255) not null,
    is_delete      bit          not null,
    is_success     bit          not null,
    product_name   varchar(255) not null,
    result         varchar(500) null,
    result_details text         null,
    status         varchar(50)  null,
    success_count  int          null,
    total_items    int          null,
    upload_date    datetime(6)  not null,
    product_id     bigint       not null,
    stall_id       bigint       not null,
    user_id        bigint       not null,
    constraint FK5bgabdvgfl1bbh4jsim458ogy
        foreign key (stall_id) references stall (id),
    constraint FKiascrff0wminrfto86fttj4qv
        foreign key (user_id) references user (id),
    constraint FKrjrdqfxgao3prxu7ua2820yse
        foreign key (product_id) references product (id)
);

create table wallet
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6)    not null,
    created_by varchar(50)    null,
    deleted_by varchar(50)    null,
    is_delete  bit            not null,
    updated_at datetime(6)    null,
    balance    decimal(15, 2) null,
    user_id    bigint         not null,
    constraint UKhgee4p1hiwadqinr0avxlq4eb
        unique (user_id),
    constraint FKbs4ogwiknsup4rpw8d47qw9dx
        foreign key (user_id) references user (id)
);

create table wallethistory
(
    id           bigint auto_increment
        primary key,
    amount       decimal(15, 2)                                     not null,
    created_at   datetime(6)                                        null,
    created_by   varchar(255)                                       null,
    deleted_by   varchar(255)                                       null,
    description  longtext                                           null,
    is_delete    bit                                                null,
    reference_id varchar(255)                                       null,
    status       enum ('CANCELED', 'FAILED', 'PENDING', 'SUCCESS')  null,
    type         enum ('DEPOSIT', 'PURCHASE', 'REFUND', 'WITHDRAW') not null,
    updated_at   datetime(6)                                        null,
    wallet_id    bigint                                             not null
);

create table warehouse
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6)                              not null,
    item_data  text                                     not null,
    item_type  enum ('ACCOUNT', 'CARD', 'EMAIL', 'KEY') not null,
    product_id bigint                                   not null,
    shop_id    bigint                                   not null,
    stall_id   bigint                                   not null,
    user_id    bigint                                   not null,
    constraint FK415xnc88aigsjbfansim72b4s
        foreign key (user_id) references user (id),
    constraint FKaf3ai1xld4bjvd2xfd70yij9u
        foreign key (product_id) references product (id),
    constraint FKdvama9icujxcgbms2k3el6xen
        foreign key (shop_id) references shop (id),
    constraint FKtd6u9pxedw45j0l1y92lm1w64
        foreign key (stall_id) references stall (id)
);

create table transaction
(
    id                bigint auto_increment
        primary key,
    amount            decimal(15, 2)                                    not null,
    buyer_id          bigint                                            not null,
    completed_at      datetime(6)                                       null,
    created_at        datetime(6)                                       not null,
    delivery_data     text                                              null,
    notes             text                                              null,
    payment_method    varchar(50)                                       null,
    product_id        bigint                                            not null,
    seller_id         bigint                                            not null,
    shop_id           bigint                                            not null,
    stall_id          bigint                                            not null,
    status            enum ('FAILED', 'PENDING', 'REFUNDED', 'SUCCESS') not null,
    transaction_code  varchar(100)                                      null,
    updated_at        datetime(6)                                       not null,
    warehouse_item_id bigint                                            not null,
    constraint UKgyh6k86an5b56kbl0e7qngoxg
        unique (transaction_code),
    constraint FK1bkeyn2xhcnk7jnxpj1xvjd5m
        foreign key (product_id) references product (id),
    constraint FK24cgkhbdnm0esh5e3belpk58k
        foreign key (shop_id) references shop (id),
    constraint FKf1y84c1e3lff5lutwnlef0rxr
        foreign key (warehouse_item_id) references warehouse (id),
    constraint FKosd6qqlkyqp8gk4gjisggqev0
        foreign key (buyer_id) references user (id),
    constraint FKrjj62bs55yjfobkii5vr8425h
        foreign key (stall_id) references stall (id),
    constraint FKs37irexq9hyvl7pqyqya2i0dn
        foreign key (seller_id) references user (id)
);

create table complaint
(
    id             bigint auto_increment
        primary key,
    created_at     datetime(6)                              not null,
    created_by     varchar(50)                              null,
    deleted_by     varchar(50)                              null,
    is_delete      bit                                      not null,
    updated_at     datetime(6)                              null,
    buyer_id       bigint                                   not null,
    description    text                                     null,
    status         enum ('APPROVED', 'PENDING', 'REJECTED') null,
    transaction_id bigint                                   not null,
    constraint FKf4a24vly15vsn7ro1nr417dly
        foreign key (buyer_id) references user (id),
    constraint FKka1oc0y7dh4bpp6whaop0nhyd
        foreign key (transaction_id) references transaction (id)
);

create table withdrawrequest
(
    id                  bigint auto_increment
        primary key,
    created_at          datetime(6)                              not null,
    created_by          varchar(50)                              null,
    deleted_by          varchar(50)                              null,
    is_delete           bit                                      not null,
    updated_at          datetime(6)                              null,
    amount              decimal(15, 2)                           not null,
    bank_account_name   varchar(100)                             not null,
    bank_account_number varchar(50)                              not null,
    bank_name           varchar(100)                             not null,
    note                varchar(255)                             null,
    shop_id             bigint                                   not null,
    status              enum ('APPROVED', 'PENDING', 'REJECTED') null,
    constraint FKmi7hlxnv0uaun8eiqdlwnsa3x
        foreign key (shop_id) references shop (id)
);

