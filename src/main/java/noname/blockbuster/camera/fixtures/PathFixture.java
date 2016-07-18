package noname.blockbuster.camera.fixtures;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects.ToStringHelper;

import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayer;
import noname.blockbuster.camera.Position;

/**
 * Path camera fixture
 *
 * This fixture is responsible for making smooth camera movements through pre
 */
public class PathFixture extends AbstractFixture
{
    protected List<Position> points = new ArrayList<Position>();

    public PathFixture(long duration)
    {
        super(duration);
    }

    public void addPoint(Position point)
    {
        this.points.add(point);
    }

    public void removePoint(int index)
    {
        this.points.remove(index);
    }

    @Override
    public void edit(String[] args, EntityPlayer player) throws CommandException
    {}

    @Override
    public void applyFixture(float progress, Position pos)
    {
        progress = progress * (this.points.size() - 1);

        int prev = (int) Math.floor(progress);
        int next = (int) Math.ceil(progress);

        Position prevPos = this.points.get(prev);
        Position nextPos = this.points.get(next);

        progress = progress - prev;

        float x = this.interpolate(prevPos.point.x, nextPos.point.x, progress);
        float y = this.interpolate(prevPos.point.y, nextPos.point.y, progress);
        float z = this.interpolate(prevPos.point.z, nextPos.point.z, progress);

        float yaw = this.interpolate(prevPos.angle.yaw, nextPos.angle.yaw, progress);
        float pitch = this.interpolate(prevPos.angle.pitch, nextPos.angle.pitch, progress);

        pos.setPosition(x, y, z);
        pos.setAngle(yaw, pitch);
    }

    private float interpolate(float a, float b, float position)
    {
        return a + (b - a) * position;
    }

    @Override
    protected ToStringHelper getToStringHelper()
    {
        return super.getToStringHelper().add("points", this.points.size());
    }
}