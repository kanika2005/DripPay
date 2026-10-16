-- Align with JPA mapping (VARCHAR). V1 used CHAR(7) which fails Hibernate validate on MySQL 9.x.
ALTER TABLE budgets MODIFY month_year VARCHAR(7) NOT NULL;
