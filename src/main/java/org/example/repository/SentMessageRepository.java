package org.example.repository;

import org.example.database.HibernateUtil;
import org.example.entity.SentMessage;
import org.hibernate.Session;

import java.util.List;

public class SentMessageRepository {
    public List<SentMessage> getLatestSentMessage(Long service_id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<SentMessage> sentMessage = null;
        try {
            session.beginTransaction();
            sentMessage = session.createQuery("FROM SentMessage WHERE service.id = :service_id AND isRead = false ORDER BY sentAt DESC", SentMessage.class)
                    .setParameter("service_id", service_id)
                    .getResultList();

            for (SentMessage message : sentMessage) {
                message.setIsRead(true);
                session.update(message);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return sentMessage;
    }
}
