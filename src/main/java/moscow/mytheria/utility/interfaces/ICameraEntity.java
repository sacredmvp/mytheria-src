package moscow.mytheria.utility.interfaces;

/**
 * Интерфейс для энтити с независимой камерой (FreeLook).
 * Позволяет хранить углы камеры отдельно от углов тела игрока.
 */
public interface ICameraEntity {
    /**
     * Получить yaw камеры (горизонтальный угол).
     */
    float getCameraYaw();
    
    /**
     * Получить pitch камеры (вертикальный угол).
     */
    float getCameraPitch();
    
    /**
     * Установить yaw камеры.
     */
    void setCameraYaw(float yaw);
    
    /**
     * Установить pitch камеры.
     */
    void setCameraPitch(float pitch);
}
