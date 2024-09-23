package org.example.repository;

import org.example.database.HibernateUtil;
import org.example.entity.DataReturn;
import org.hibernate.Session;

public class DataReturnRepository {
    public DataReturn getLatestDataReturn(Long service_id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        DataReturn dataReturn = null;
        try {
            session.beginTransaction();
            dataReturn = session.createQuery("FROM DataReturn WHERE service.id = :service_id ORDER BY createdAt DESC", DataReturn.class)
                    .setParameter("service_id", service_id)
                    .setMaxResults(1)
                    .uniqueResult();


            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return dataReturn;
    }
}
