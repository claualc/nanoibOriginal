CREATE DATABASE nanoib;

USE nanoib;


/*
Tabela de contas, com informações tambem do usuário
*/
DELIMITER $$

create table if not exists accounts(
  branch_id bigint not null default 0,
  id bigint not null default 0,
  holders_name varchar(40) not null,
  pwd binary(32) not null,
  pwd_salt binary(8) not null,
  primary key (branch_id, id)
);

$$


/*
Tabela de saldos de conta.
*/
DELIMITER $$

create table if not exists balances(
  acc_branch_id bigint not null default 0,
  acc_id bigint not null default 0,
  balance numeric(20,2) not null default 0.0,
  op_count  bigint not null default 0,
  primary key (acc_branch_id, acc_id),
  foreign key (acc_branch_id, acc_id) references accounts(branch_id, id)
);

$$


/*
Tabela de itens de extrato
*/
DELIMITER $$

create table if not exists statement_items(
  acc_branch_id bigint not null default 0,
  acc_id bigint not null default 0,
  op_count  bigint not null default 0,
  tx_timestamp timestamp not null,
  in_out tinyint not null default 0,
  value numeric(20,2) not null default 0.0,
  comment varchar(40) not null,
  ctpt_acc_branch_id bigint not null default 0,
  ctpt_acc_id bigint not null default 0,
  foreign key (acc_branch_id, acc_id) references accounts(branch_id, id),
  foreign key (ctpt_acc_branch_id, ctpt_acc_id) references accounts(branch_id, id),
  index(tx_timestamp)
);

$$


/*
Procedure para criar conta e saldo transacionalmente
*/
DELIMITER $$

create or replace procedure create_account(
  in p_branch_id bigint,
  in p_id bigint,
  in p_holders_name varchar(40),
  in p_pwd_salt binary(8),
  in p_pwd binary(32),
  in p_initial_balance numeric(20,2)
)
begin
  declare exit handler for sqlexception
    begin
      rollback;
      resignal;
    end;

  start transaction;
    insert into accounts (
      branch_id,
      id,
      holders_name,
      pwd,
      pwd_salt
    ) values (
      p_branch_id,
      p_id,
      p_holders_name,
      p_pwd,
      p_pwd_salt
    );

    insert into balances (
      acc_branch_id,
      acc_id,
      balance,
      op_count
    ) values (
      p_branch_id,
      p_id,
      p_initial_balance,
      0
    );
  commit;
end;

$$


/*
Consulta de usuário
*/
DELIMITER $$

create or replace procedure get_user(
  in p_acc_branch_id bigint,
  in p_acc_id bigint
) reads sql data
begin
  select
    holders_name,
    pwd_salt,
    pwd
  from
    accounts
  where
    branch_id = p_acc_branch_id
    and
    id = p_acc_id;
end;

$$


/*
Procedure para consulta de saldo
*/
DELIMITER $$

create or replace procedure get_balance(
  in p_acc_branch_id bigint,
  in p_acc_id bigint
) reads sql data
begin
  select
    balance,
    op_count
  from
    balances
  where
    acc_branch_id = p_acc_branch_id
    and
    acc_id = p_acc_id;
end;

$$


/*
Procedure para transferência de valores entre saldos de contas, e inserção dos respectivos
itens de extrato, de forma transacional
*/
DELIMITER $$

create or replace procedure transfer(
  in p_orig_acc_branch_id bigint,
  in p_orig_acc_id bigint,
  in p_dest_acc_branch_id bigint,
  in p_dest_acc_id bigint,
  in p_value numeric(20,2),
  in p_comment varchar(40)
)
begin
  declare v_orig_acc_balance numeric(20,2);
  declare v_orig_acc_opcount bigint;
  declare v_dest_acc_opcount bigint;
  declare v_tx_ts timestamp;

  declare exit handler for sqlexception
    begin
      rollback;
      resignal;
    end;

  declare exit handler for sqlwarning
    begin
      rollback;
      resignal;
    end;

  start transaction;
    if p_value <= 0 then
      signal sqlstate '45000' set message_text = 'Neg. Input Value';
    end if;

    if p_orig_acc_branch_id = p_dest_acc_branch_id and p_orig_acc_id = p_dest_acc_id then
      signal sqlstate '45000' set message_text = 'Same Orig. & Dest. Accs.';
    end if;

    set @v_orig_acc_opcount := null;

    select
      @v_orig_acc_balance := balance,
      @v_orig_acc_opcount := op_count
    from balances where acc_branch_id = p_orig_acc_branch_id and acc_id = p_orig_acc_id for update;

    if @v_orig_acc_opcount is null then
      signal sqlstate '45000' set message_text = 'Unk. Orig. Acc.';
    end if;

    if @v_orig_acc_balance < p_value then
      signal sqlstate '45000' set message_text = 'Insuf. Funds';
    end if;

    set @v_dest_acc_opcount := null;

    select
      @v_dest_acc_opcount := op_count
    from balances where acc_branch_id = p_dest_acc_branch_id and acc_id = p_dest_acc_id for update;

    if @v_dest_acc_opcount is null then
      signal sqlstate '45000' set message_text = 'Unk. Dest. Acc.';
    end if;

    set @v_tx_ts := current_timestamp();

    set @v_orig_acc_opcount := @v_orig_acc_opcount + 1;
    set @v_dest_acc_opcount := @v_dest_acc_opcount + 1;

    update balances set
      balance = balance - p_value,
      op_count = @v_orig_acc_opcount
    where acc_branch_id = p_orig_acc_branch_id and acc_id = p_orig_acc_id;

    update balances set
      balance = balance + p_value,
      op_count = @v_dest_acc_opcount
    where acc_branch_id = p_dest_acc_branch_id and acc_id = p_dest_acc_id;

    insert into statement_items(
      acc_branch_id,
      acc_id,
      op_count,
      tx_timestamp,
      in_out,
      value,
      comment,
      ctpt_acc_branch_id,
      ctpt_acc_id
    ) values (
      p_orig_acc_branch_id,
      p_orig_acc_id,
      @v_orig_acc_opcount,
      @v_tx_ts,
      0,
      p_value,
      p_comment,
      p_dest_acc_branch_id,
      p_dest_acc_id
    ),(
      p_dest_acc_branch_id,
      p_dest_acc_id,
      @v_dest_acc_opcount,
      @v_tx_ts,
      1,
      p_value,
      p_comment,
      p_orig_acc_branch_id,
      p_orig_acc_id
    );

  commit;
end;

$$


/*
Procedure para contagem dos itens de extrato de uma conta
*/
DELIMITER $$

create or replace procedure count_statement_items(
  in p_acc_branch_id bigint,
  in p_acc_id bigint,
  out p_count int
)
begin
  select
    count(*) into p_count
  from
    statement_items
  where
    acc_branch_id = p_acc_branch_id
    and
    acc_id = p_acc_id;
end;

$$


/*
Procedure para seleção paginada dos itens de extrato de uma conta
*/
DELIMITER $$

create or replace procedure get_statement_items(
  in p_acc_branch_id bigint,
  in p_acc_id bigint,
  in p_offset int,
  in p_result_max_size int
) reads sql data
begin
  select
    t1.op_count,
    t1.tx_timestamp,
    t1.in_out,
    t1.value,
    t1.comment,
    t1.ctpt_acc_branch_id,
    t1.ctpt_acc_id,
    t2.holders_name
  from
    statement_items as t1
  join accounts as t2
    on
      t2.branch_id = t1.ctpt_acc_branch_id
    and 
      t2.id = t1.ctpt_acc_id
  where
      t1.acc_branch_id = p_acc_branch_id
    and
      t1.acc_id = p_acc_id
  order by
    t1.op_count desc
  limit
    p_offset, p_result_max_size;
end;

$$


call create_account(0, 0, 'John Doe', 0xBE0FCCDBBCB4848D, 0xE6B7EF8854E8B01FCF7B7AFBEB9E03BAA9D09A88F23E218EDE91AC503BBD0211, 50000.00);
call create_account(0, 1, 'Richard Roe', 0x02C2C080258298AB, 0x38A743D7B751EAA937290E88E458E53F73A08964A6B4081336E61AE685942F5D, 50000.00);
call create_account(0, 2, 'Janie Roe', 0x29D0B1B53360CD9D, 0x323E6DF32E7BF5D491F8DC61E0853607AA0CC0255E683981DEC8726FD1E1A20A, 50000.00);
call create_account(0, 3, 'Baby Doe', 0x1A89299556104D9C, 0x6D8C6208C0335406D787BFAE8F29562129140665E87A92258790C5A9AF129B8D, 50000.00);


CREATE USER 'nanoibserver'@'localhost' IDENTIFIED BY 'pwd';

GRANT EXECUTE ON PROCEDURE nanoib.get_user TO 'nanoibserver'@'localhost';
GRANT EXECUTE ON PROCEDURE nanoib.get_balance TO 'nanoibserver'@'localhost';
GRANT EXECUTE ON PROCEDURE nanoib.transfer TO 'nanoibserver'@'localhost';
GRANT EXECUTE ON PROCEDURE nanoib.count_statement_items TO 'nanoibserver'@'localhost';
GRANT EXECUTE ON PROCEDURE nanoib.get_statement_items TO 'nanoibserver'@'localhost';
